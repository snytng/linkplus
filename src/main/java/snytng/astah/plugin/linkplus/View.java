package snytng.astah.plugin.linkplus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.presentation.IPresentation;
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
		consoleHandler.setLevel(Level.WARNING);
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
	JTable linksTable = null;
	JScrollPane scrollPane = null;
	JTextField keywordTextField = null;
	JComboBox<String> searchOptions = null;
	JComboBox<String> searchTypes = null;

	class Link {
		String label;
		IPresentation presentation;
		IDiagram diagram;

		Link(IPresentation presentation, IDiagram diagram){
			this.label = presentation.getLabel();
			this.presentation = presentation;
			this.diagram = diagram;
		}

		Link(String label, IPresentation presentation, IDiagram diagram){
			this.label = label;
			this.presentation = presentation;
			this.diagram = diagram;
		}

		public String getLabel() {
			return label;
		}

		public String getPackageName() {
			StringBuilder sb = new StringBuilder();
			IElement owner = diagram.getOwner();
			while (owner instanceof INamedElement && owner.getOwner() != null) {
				sb.insert(0, ((INamedElement) owner).getName() + "::");
				owner = owner.getOwner();
			}
			return sb.toString();
		}

		public String toString() {
			return getLabel();
		}


	}

	private transient List<Link> links = new ArrayList<>();

	private String[] columnNames = new String[]{"ノード", "属性", "ダイアグラム", "パス"};

	@SuppressWarnings("serial")
	private DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
		// セル編集できないようにする
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
	};

	private static final String SEARCH_TYPE_NOTE  = "Note";
	private static final String SEARCH_TYPE_TOPIC = "Topic";
	private static final String SEARCH_TYPE_CLASS = "Class";
	private static final String SEARCH_TYPE_ALL   = "全て";

	private static final String SEARCH_OPTION_STARTSWITH = "前方一致";
	private static final String SEARCH_OPTION_CONTAINS   = "含む";
	private static final String SEARCH_OPTION_COLOR   = "色一致";

	private static final String FONT_COLOR = "font.color";


	public Container createPane() {

		linksTable = new JTable(tableModel);
		linksTable.setRowSelectionAllowed(true);    // 行選択を可能にする
		linksTable.setColumnSelectionAllowed(true); // 列選択を可能にする
		linksTable.setCellSelectionEnabled(true);   // セル選択を可能にする
		linksTable.setAutoCreateRowSorter(true);    // ソートを可能にする
		linksTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		linksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		linksTable.getSelectionModel().addListSelectionListener(e -> {
			logger.log(Level.INFO, "ListSelectionListener");
			showDiagram();
			linksTable.requestFocusInWindow();
		});
		linksTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
				logger.log(Level.INFO, "TableColumnModelListener columnSelectionChanged");
				showDiagram();
				linksTable.requestFocusInWindow();
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
				// no action
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
				// no action
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				// no action
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {
				// no action
			}
		});

		// ヘッダーをShiftキーを押しながらクリックするとソート状態を解除する
		linksTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				final RowSorter<? extends TableModel> sorter = linksTable.getRowSorter();
				if (sorter == null || sorter.getSortKeys().isEmpty()) {
					return;
				}
				JTableHeader h = (JTableHeader) e.getComponent();
				TableColumnModel columnModel = h.getColumnModel();
				int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				if (viewColumn < 0) {
					return;
				}
				int column = columnModel.getColumn(viewColumn).getModelIndex();
				if (column != -1 && e.isShiftDown()) {
					EventQueue.invokeLater(new Runnable() {
						@Override public void run() {
							sorter.setSortKeys(null);
						}
					});
				}
			}
		});

		scrollPane = new JScrollPane(
				linksTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JPanel keywordPanel = new JPanel();
		JLabel keywordLabel = new JLabel("キーワード");
		keywordTextField = new JTextField("TODO", 10);
		keywordTextField.addKeyListener(new KeyListener() {
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
		keywordPanel.add(keywordLabel);
		keywordPanel.add(keywordTextField);

		searchOptions = new JComboBox<>(new String[] {
				SEARCH_OPTION_STARTSWITH,
				SEARCH_OPTION_CONTAINS,
				SEARCH_OPTION_COLOR
		});
		searchOptions.addActionListener(e -> {
			if(! searchOptions.getSelectedItem().equals(SEARCH_OPTION_COLOR)){
				keywordTextField.setBackground(Color.WHITE);
				selectedFontColor = FONT_COLOR_BLACK;
			}

			updateDiagramView();
			});

		searchTypes = new JComboBox<>(new String[] {
				SEARCH_TYPE_NOTE,
				SEARCH_TYPE_TOPIC,
				SEARCH_TYPE_CLASS,
				SEARCH_TYPE_ALL
		});
		searchTypes.addActionListener(e -> updateDiagramView());

		menuPanel.add(keywordPanel);
		menuPanel.add(searchOptions);
		menuPanel.add(searchTypes);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(menuPanel, BorderLayout.NORTH);
		topPanel.add(scrollPane, BorderLayout.CENTER);

		return topPanel;
	}

	private void showDiagram() {
		int row = linksTable.getSelectedRow();
		int col = linksTable.getSelectedColumn();

		logger.log(Level.INFO, "notesTable select (" + row + "," + col + ")");

		// 未選択状態
		if(row < 0) {
			return;
		}

		Link link = (Link)linksTable.getValueAt(row, 0);
		diagramViewManager.open(link.diagram);
		if(col == 0) {
			diagramViewManager.select(link.presentation);
			diagramViewManager.showInDiagramEditor(link.presentation);
		} else {
			diagramViewManager.unselectAll();
		}
	}

	private void getPresentationsWithLabel(String keyword) {
		links = new ArrayList<>();
		try {
			IPackage root = projectAccessor.getProject();
			List<IPackage> ps = new ArrayList<>();
			ps.add(root);
			getPackages(root, ps);

			List<IDiagram> ds = ps.stream()
					.flatMap(p -> Stream.of(p.getDiagrams()))
					.collect(Collectors.toList());

			// ダイアグラムを巡回
			for(IDiagram d : ds) {

				// Presentationを登録
				Stream.of(d.getPresentations())
				.filter(p -> filterType(p.getType()))
				.filter(p -> filterLabel(p.getLabel(), keyword))
				.filter(p -> filterFontColor(p, keywordTextField.getBackground()))
				.forEach(p -> links.add(new Link(p, d)));

				// 全て洗濯した場合には、クラス要素とステレオタイプを登録
				Stream.of(d.getPresentations())
				.filter(p -> searchTypes.getSelectedItem().equals(SEARCH_TYPE_ALL))
				.filter(p -> ! searchOptions.getSelectedItem().equals(SEARCH_OPTION_COLOR)) // 色選択は除外
				.forEach(p -> {
					// クラスの場合にはメソッドと属性を確認
					if(p.getType().equals("Class")) {
						IClass c = (IClass)p.getModel();

						// メソッド
						Stream.of(c.getOperations())
						.filter(o -> filterLabel(o.getName(), keyword))
						.forEach(o -> {
							links.add(new Link(o.getName(), p, d));
						});

						// 属性
						Stream.of(c.getAttributes())
						.filter(a -> filterLabel(a.getName(), keyword))
						.forEach(a -> {
							links.add(new Link(a.getName(), p, d));
						});
					}

					// ステレオタイプを確認
					if(p.getModel() != null) {
						Stream.of(p.getModel().getStereotypes())
						.filter(s -> filterLabel(s, keyword))
						.forEach(s -> links.add(new Link(s, p, d)));
					}

				});
			}

		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	private boolean filterType(String type) {
		if(type == null) {
			return false;
		}

		return searchTypes.getSelectedItem().equals(SEARCH_TYPE_ALL) ||
				type.equals(searchTypes.getSelectedItem());
	}

	private boolean filterLabel(String label, String keyword) {
		if(label == null || keyword == null){
			return false;
		}

		if(label.isEmpty()) {
			return false;
		}

		if(keyword.isEmpty()) {
			return true;
		}

		if(searchOptions.getSelectedItem().equals(SEARCH_OPTION_STARTSWITH)) {
			return label.toLowerCase().startsWith(keyword.toLowerCase());

		} else if(searchOptions.getSelectedItem().equals(SEARCH_OPTION_CONTAINS)){
			return label.toLowerCase().contains(keyword.toLowerCase());

		} else if(searchOptions.getSelectedItem().equals(SEARCH_OPTION_COLOR)){
			return true;

		}else {
			return false;
		}
	}

	private static final String FONT_COLOR_BLACK = "#000000";
	private String selectedFontColor = FONT_COLOR_BLACK;

	private boolean filterFontColor(IPresentation p, Color filterColor) {
		if(searchOptions.getSelectedItem().equals(SEARCH_OPTION_COLOR)) {
			String fontColor = p.getProperty(FONT_COLOR);
			return selectedFontColor.equals(fontColor);

		} else {
			return true;
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

			getPresentationsWithLabel(keywordTextField.getText());

			tableModel.setRowCount(0);

			logger.log(Level.INFO, () -> "links.size=" + links.size());
			links.stream()
			.map(l -> {
				String type  = l.presentation.getType();
				String name  = l.diagram.getName();
				String path  = l.getPackageName();
				return new Object[] {l, type, name, path};
			})
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

		if(! searchOptions.getSelectedItem().equals(SEARCH_OPTION_COLOR)) {
			return;
		}

		IPresentation[] ps = diagramViewManager.getSelectedPresentations();
		if (ps.length == 0) {
			keywordTextField.setBackground(Color.WHITE);
			selectedFontColor = FONT_COLOR_BLACK;
			return;
		}

		IPresentation p = ps[0];

		String fontColor = p.getProperty(FONT_COLOR);
		if(fontColor.equals("null")) {
			return;
		}
		this.selectedFontColor = fontColor;

		int r = Integer.decode("0x" + fontColor.substring(1,3));
		int g = Integer.decode("0x" + fontColor.substring(3,5));
		int b = Integer.decode("0x" + fontColor.substring(5,7));

		keywordTextField.setBackground(new Color(r,g,b));
		keywordTextField.setText("");

		updateDiagramView();
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
