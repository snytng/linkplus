package snytng.astah.plugin.linkplus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IMindMapDiagram;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectEvent;
import com.change_vision.jude.api.inf.project.ProjectEventListener;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IEntitySelectionEvent;
import com.change_vision.jude.api.inf.view.IEntitySelectionListener;


public class View
extends
JPanel
implements
IPluginExtraTabView,
IEntitySelectionListener,
IDiagramEditorSelectionListener,
ProjectEventListener
{
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(View.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.INFO);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	/**
	 * プロパティファイルの配置場所
	 */
	private static final String VIEW_PROPERTIES = "snytng.astah.plugin.linkplus.view";

	/**
	 * リソースバンドル
	 */
	private static final ResourceBundle VIEW_BUNDLE = ResourceBundle.getBundle(VIEW_PROPERTIES, Locale.getDefault());

	private String title = "<linkplus>";
	private String description = "<This plugin lists notes with a specific string and shows diagram of them.>";

	private static final long serialVersionUID = 1L;
	private transient ProjectAccessor projectAccessor = null;
	private transient IDiagramViewManager diagramViewManager = null;

	public View() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
			diagramViewManager = projectAccessor.getViewManager().getDiagramViewManager();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		initProperties();
		initComponents();
	}

	private void initProperties() {
		try {
			title = VIEW_BUNDLE.getString("pluginExtraTabView.title");
			description = VIEW_BUNDLE.getString("pluginExtraTabView.description");
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void initComponents() {
		setLayout(new GridLayout(1,1));
		add(createPane());
	}

	JPanel scrollPanel = null;
	JTable notesTable = null;
	JScrollPane scrollPane = null;
	JTextField prefixTextField = null;

	class Link {
		INodePresentation node;
		IDiagram diagram;

		Link(INodePresentation node, IDiagram diagram){
			this.node = node;
			this.diagram = diagram;
		}
	}

	private transient List<Link> links = new ArrayList<>();

	private String[] columnNames = new String[]{"ノート", "属性", "ダイアグラム"};

	private DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
		// セル編集できないようにする
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
	};

	public Container createPane() {

		notesTable = new JTable(tableModel);
		notesTable.setColumnSelectionAllowed(true);
		notesTable.setAutoCreateRowSorter(true);
		notesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		notesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		ListSelectionModel selectionModel = notesTable.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) {
				return;
			}
			showDiagram();
			notesTable.requestFocusInWindow();
		});

		scrollPane = new JScrollPane(
				notesTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new BorderLayout());

		JPanel prefixPanel = new JPanel();
		JLabel prefixLabel = new JLabel("キーワード");
		prefixTextField = new JTextField("TODO", 10);
		prefixTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// no action
			}

			@Override
			public void keyReleased(KeyEvent e) {
				updateDiagramView();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// no action
			}
		});
		prefixPanel.add(prefixLabel);
		prefixPanel.add(prefixTextField);

		menuPanel.add(prefixPanel, BorderLayout.WEST);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(menuPanel, BorderLayout.NORTH);
		topPanel.add(scrollPane, BorderLayout.CENTER);

		return topPanel;
	}

	private void showDiagram() {
		int row = notesTable.getSelectedRow();
		int col = notesTable.getSelectedColumn();

		if(row < 0) {
			return;
		}

		diagramViewManager.open(links.get(row).diagram);
		if(col == 0) {
			diagramViewManager.showInDiagramEditor(links.get(row).node);
			//diagramViewManager.unselectAll();
		}
	}

	private void getNodes(String prefix) {
		links = new ArrayList<>();
		try {
			IPackage root = projectAccessor.getProject();
			List<IPackage> ps = new ArrayList<>();
			ps.add(root);
			getPackages(root, ps);

			List<IDiagram> ds = ps.stream()
			.flatMap(p -> Stream.of(p.getDiagrams()))
			.collect(Collectors.toList());

			for(IDiagram d : ds) {
				// ノートを追加
				Stream.of(d.getPresentations())
				.filter(INodePresentation.class::isInstance)
				.map(INodePresentation.class::cast)
				.filter(np -> np.getType().equals("Note"))
				.filter(np -> np.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
				.forEach(np -> links.add(new Link(np, d)));

				// マインドマップのノードを追加
				if(d instanceof IMindMapDiagram) {
					Stream.of(d.getPresentations())
					.filter(INodePresentation.class::isInstance)
					.map(INodePresentation.class::cast)
					.filter(np -> np.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
					.forEach(np -> links.add(new Link(np, d)));
				}
			}



		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	public void getPackages(IPackage iPackage, List<IPackage> iPackages) {
	     INamedElement[] iNamedElements = iPackage.getOwnedElements();
	     for (INamedElement iNamedElement : iNamedElements) {
	        if (iNamedElement instanceof IPackage) {
	            iPackages.add((IPackage)iNamedElement);
	            getPackages((IPackage)iNamedElement, iPackages);
	        }
	      }
	}

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView() {
		try {
			logger.log(Level.INFO, "update diagram view.");

			getNodes(prefixTextField.getText());

			tableModel.setRowCount(0);

			logger.log(Level.INFO, () -> "links.size=" + links.size());
			links.stream()
			.map(l -> new String[] {l.node.getLabel(), l.node.getType(), l.diagram.getName()})
			.forEach(tableModel::addRow);

			tableModel.fireTableDataChanged();

		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * 図の選択が変更されたら表示を更新する
	 */
	@Override
	public void diagramSelectionChanged(IDiagramEditorSelectionEvent e) {
		logger.log(Level.INFO, "diagramSelectionChanged");
	}

	/**
	 * 要素の選択が変更されたら表示を更新する
	 */
	@Override
	public void entitySelectionChanged(IEntitySelectionEvent e) {
		logger.log(Level.INFO, "entitySelectionChanged");
	}

	// ProjectEventListener
	/**
	 * 図が編集されたら表示を更新する
	 * @see com.change_vision.jude.api.inf.project.ProjectEventListener#projectChanged(com.change_vision.jude.api.inf.project.ProjectEvent)
	 */
	@Override
	public void projectChanged(ProjectEvent e) {
		logger.log(Level.INFO, "projectChanged");
		updateDiagramView();
	}

	@Override
	public void projectClosed(ProjectEvent e) {
		// Do nothing
	}

	@Override
	public void projectOpened(ProjectEvent e) {
		// Do nothing
	}

	// IPluginExtraTabView
	@Override
	public void addSelectionListener(ISelectionListener listener) {
		// Do nothing
	}

	@Override
	public void activated() {
		// リスナーへの登録
		addDiagramListeners();
		// 再表示されたら表示を更新
		updateDiagramView();
	}

	@Override
	public void deactivated() {
		// リスナーへの削除
		removeDiagramListeners();
	}

	private void addDiagramListeners(){
		diagramViewManager.addDiagramEditorSelectionListener(this);
		diagramViewManager.addEntitySelectionListener(this);
		projectAccessor.addProjectEventListener(this);
	}

	private void removeDiagramListeners(){
		diagramViewManager.removeDiagramEditorSelectionListener(this);
		diagramViewManager.removeEntitySelectionListener(this);
		projectAccessor.removeProjectEventListener(this);
	}


	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getTitle() {
		return title;
	}


}
