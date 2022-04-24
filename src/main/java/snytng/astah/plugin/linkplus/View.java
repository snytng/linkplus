package snytng.astah.plugin.linkplus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
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
	private static final String VIEW_PROPERTIES = View.class.getPackage().getName() + ".view";

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


	JPanel scrollPanel = null;
	JTable linksTable = null;
	JScrollPane scrollPane = null;
	JTextField keywordTextField = null;
	JComboBox<String> searchOptions = null;
	JComboBox<String> searchTypes = null;
	JButton fontColorButton = null;
	JComboBox<String> searchColors = null;
	JComboBox<String> searchDiagrams = null;

	private enum SEARCH_TYPE {
		ALL("すべて"),
		NOTE("Note"),
		TOPIC("Topic"),
		CLASS("Class");

		private final String text;
		private SEARCH_TYPE(final String text) {
			this.text = text;
		}
	}

	private static final String INITIAL_SEARCH_KEYWORD = "TODO";
	private enum SEARCH_OPTION {
		STARTSWITH("前方一致"),
		CONTAINS("含む");

		private final String text;
		private SEARCH_OPTION(final String text) {
			this.text = text;
		}
	}

	private enum SEARCH_FONT {
		COLOR_ALL("すべて"),
		COLOR_MATCH("同じ"),
		NOT_MATCH("違う");

		private final String text;
		private SEARCH_FONT(final String text) {
			this.text = text;
		}
	}

	private enum SEARCH_DIAGRAM {
		ALL("すべて"),
		CURRENT("今の図"),
		PACKAGE("パッケージ");

		private final String text;
		private SEARCH_DIAGRAM(final String text) {
			this.text = text;
		}

	}

	private static final String PROPERTY_FONT_COLOR = "font.color";
	private static final String FONT_COLOR_BLACK = "#000000";
	private String selectedFontColor = FONT_COLOR_BLACK;

	private boolean isAtRow0inLinksTable = false;

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
			// 選択操作中は何もしない
			if (e.getValueIsAdjusting()) {
				return;
			}
			showDiagram();
			linksTable.requestFocusInWindow();
		});

		linksTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
				logger.log(Level.INFO, "TableColumnModelListener columnSelectionChanged");
				// 選択操作中は何もしない
				if (e.getValueIsAdjusting()) {
					return;
				}
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

		linksTable.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// no action
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// no action
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// linksTableの一番上のセルでUPキーを押したら、
				// keywordTextFieldへフォーカスを移動する
				if (e.getKeyCode() == KeyEvent.VK_UP
						&&
						isAtRow0inLinksTable) {
					linksTable.clearSelection();
					keywordTextField.requestFocusInWindow();
				}
				// linksTableの一番上のセルかどうかを確認
				if(linksTable.getSelectedRow() == 0) {
					isAtRow0inLinksTable = true;
				} else {
					isAtRow0inLinksTable = false;
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
		keywordTextField = new JTextField(INITIAL_SEARCH_KEYWORD, 10);
		keywordTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				// no action
			}

			@Override
			public void keyReleased(KeyEvent e) {
				update();
				// DOWNキーを押したら、linksTableの一番左上にフォーカスを移動する
				if (e.getKeyCode() == KeyEvent.VK_DOWN
						&&
						linksTable.getRowCount() > 0) {
					linksTable.setRowSelectionInterval(0,  0);
					linksTable.setColumnSelectionInterval(0,  0);
					isAtRow0inLinksTable = true;
					linksTable.requestFocusInWindow();
					return;
				}
				keywordTextField.requestFocusInWindow();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// no action
			}
		});
		keywordPanel.add(keywordLabel);
		keywordPanel.add(keywordTextField);

		searchOptions = new JComboBox<>(
				Arrays.stream(SEARCH_OPTION.values())
				.map(v -> v.text).toArray(String[]::new)
				);
		searchOptions.addActionListener(e -> update());

		JLabel searchTypesLabel = new JLabel("要素");
		searchTypes = new JComboBox<>(
				Arrays.stream(SEARCH_TYPE.values())
				.map(v -> v.text).toArray(String[]::new)
				);
		searchTypes.addActionListener(e -> update());

		fontColorButton = new JButton("文字色");
		fontColorButton.setForeground(Color.BLACK);
		fontColorButton.addActionListener(e -> {
			IPresentation[] ps = diagramViewManager.getSelectedPresentations();
			if (ps.length == 0) {
				return;
			}

			IPresentation p = ps[0];

			String fontColor = p.getProperty(PROPERTY_FONT_COLOR);
			if(fontColor.equals("null")) {
				return;
			}
			this.selectedFontColor = fontColor;

			int r = Integer.decode("0x" + fontColor.substring(1,3));
			int g = Integer.decode("0x" + fontColor.substring(3,5));
			int b = Integer.decode("0x" + fontColor.substring(5,7));

			fontColorButton.setForeground(new Color(r,g,b));

			update();
		});

		fontColorButton.setMnemonic(KeyEvent.VK_C);

		searchColors = new JComboBox<>(
				Arrays.stream(SEARCH_FONT.values())
				.map(v -> v.text).toArray(String[]::new)
				);
		searchColors.addActionListener(e -> update());

		JLabel searchDiagramsLabel = new JLabel("対象図");
		searchDiagrams= new JComboBox<>(
				Arrays.stream(SEARCH_DIAGRAM.values())
				.map(v -> v.text).toArray(String[]::new)
				);
		searchDiagrams.addActionListener(e -> update());

		menuPanel.add(keywordPanel);
		menuPanel.add(searchOptions);

		menuPanel.add(getSeparator());
		menuPanel.add(searchTypesLabel);
		menuPanel.add(searchTypes);

		menuPanel.add(getSeparator());
		menuPanel.add(fontColorButton);
		menuPanel.add(searchColors);

		menuPanel.add(getSeparator());
		menuPanel.add(searchDiagramsLabel);
		menuPanel.add(searchDiagrams);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(menuPanel, BorderLayout.NORTH);
		topPanel.add(scrollPane, BorderLayout.CENTER);

		return topPanel;
	}

	private JSeparator getSeparator(){
		return new JSeparator(SwingConstants.VERTICAL){
			@Override public Dimension getPreferredSize() {
				return new Dimension(1, 16);
			}
			@Override public Dimension getMaximumSize() {
				return this.getPreferredSize();
			}
		};
	}

	private transient IDiagram lastSelectedDiagram = null;

	private void showDiagram() {
		int row = linksTable.getSelectedRow();
		int col = linksTable.getSelectedColumn();

		logger.log(Level.INFO, () -> "notesTable select (" + row + "," + col + ")");

		try {
			if(lastSelectedDiagram != null) {
				diagramViewManager.clearAllViewProperties(lastSelectedDiagram);
			}

			// 未選択状態
			if(row < 0) {
				return;
			}

			Link link = (Link)linksTable.getValueAt(row, 0);

			diagramViewManager.open(link.diagram);
			diagramViewManager.unselectAll();
			diagramViewManager.select(link.presentation);
			diagramViewManager.showInDiagramEditor(link.presentation);

			diagramViewManager.clearAllViewProperties(link.diagram);
			lastSelectedDiagram = link.diagram;

			diagramViewManager.getViewProperties(link.presentation).keySet().stream()
			.forEach(k -> {
				try {
					String message = "key:" + k + "=" + diagramViewManager.getViewProperty(link.presentation, k);
					logger.log(Level.INFO, () -> message);
				}catch(InvalidUsingException e) {
					e.printStackTrace();
				}
			});

			diagramViewManager.setViewProperty(
					link.presentation,
					IDiagramViewManager.BORDER_COLOR,
					Color.MAGENTA);

		}catch(InvalidUsingException e) {
			e.printStackTrace();
		}
	}


	private void getPresentationsInAllDiagramsWithLabel(String keyword) {
		try {
			IPackage root = projectAccessor.getProject();

			List<IPackage> ps = new ArrayList<>();
			ps.add(root);
			getPackages(root, ps);

			List<IDiagram> ds = ps.stream()
					.flatMap(p -> Stream.of(p.getDiagrams()))
					.collect(Collectors.toList());

			getPresentationsWithLabel(ds, keyword);

		}catch(Exception e) {
		}
	}

	private void getPresentationsInDiagramsOfSamePackageWithLabel(String keyword) {
		try {
			IDiagram d = diagramViewManager.getCurrentDiagram();

			if (d == null) {
				return;
			}

			List<IPackage> ps = new ArrayList<>();
			ps.add((IPackage)d.getOwner());

			List<IDiagram> ds = ps.stream()
					.flatMap(p -> Stream.of(p.getDiagrams()))
					.collect(Collectors.toList());

			getPresentationsWithLabel(ds, keyword);

		}catch(Exception e) {
		}
	}

	private void getPresentationsInCurrentDiagramWithLabel(String keyword) {
		try {
			IDiagram d = diagramViewManager.getCurrentDiagram();

			if (d == null) {
				return;
			}

			List<IDiagram> ds = new ArrayList<>();
			ds.add(d);

			getPresentationsWithLabel(ds, keyword);

		}catch(Exception e) {
		}
	}

	private void getPresentationsWithLabel(List<IDiagram> ds, String keyword) {
		links = new ArrayList<>();
		try {
			// ダイアグラムを巡回
			for(IDiagram d : ds) {

				// 色選択の場合
				if (! searchColors.getSelectedItem().equals(SEARCH_FONT.COLOR_ALL.text)) {
					// Presentationを登録
					Stream.of(d.getPresentations())
					.filter(p -> filterType(p.getType()))
					.filter(p -> filterLabel(p.getLabel(), keyword))
					.filter(this::filterFontColor)
					.forEach(p -> links.add(new Link(p, d)));
				}
				// 色選択でない場合
				else {
					// Presentationを登録
					Stream.of(d.getPresentations())
					.filter(p -> filterType(p.getType()))
					.filter(p -> filterLabel(p.getLabel(), keyword))
					.forEach(p -> links.add(new Link(p, d)));

					// 全てを選択した場合には、クラス要素とステレオタイプを登録
					Stream.of(d.getPresentations())
					.filter(p -> searchTypes.getSelectedItem().equals(SEARCH_TYPE.ALL.text))
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
			}
		} catch(Exception e) {
		}

	}

	private boolean filterType(String type) {
		if(type == null) {
			return false;
		}

		return searchTypes.getSelectedItem().equals(SEARCH_TYPE.ALL.text) ||
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

		if(searchOptions.getSelectedItem().equals(SEARCH_OPTION.STARTSWITH.text)) {
			return label.toLowerCase().startsWith(keyword.toLowerCase());

		} else if(searchOptions.getSelectedItem().equals(SEARCH_OPTION.CONTAINS.text)){
			return label.toLowerCase().contains(keyword.toLowerCase());

		} else {
			return false;
		}
	}

	private boolean filterFontColor(IPresentation p) {
		if(searchColors.getSelectedItem().equals(SEARCH_FONT.COLOR_ALL.text)) {
			return true;
		}

		String fontColor = p.getProperty(PROPERTY_FONT_COLOR);
		if(searchColors.getSelectedItem().equals(SEARCH_FONT.COLOR_MATCH.text)){
			return selectedFontColor.equals(fontColor);
		} else {
			return ! selectedFontColor.equals(fontColor);
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
	private void update() {
		try {
			logger.log(Level.INFO, "update view.");

			if(searchDiagrams.getSelectedItem().equals(SEARCH_DIAGRAM.CURRENT.text)){
				getPresentationsInCurrentDiagramWithLabel(keywordTextField.getText());
			} else if(searchDiagrams.getSelectedItem().equals(SEARCH_DIAGRAM.PACKAGE.text)){
				getPresentationsInDiagramsOfSamePackageWithLabel(keywordTextField.getText());
			} else {
				getPresentationsInAllDiagramsWithLabel(keywordTextField.getText());
			}

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
	}

	// ProjectEventListener
	/**
	 * 図が編集されたら表示を更新する
	 * @see com.change_vision.jude.api.inf.project.ProjectEventListener#projectChanged(com.change_vision.jude.api.inf.project.ProjectEvent)
	 */
	@Override
	public void projectChanged(ProjectEvent e) {
		logger.log(Level.INFO, "projectChanged");
		update();
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
		update();
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
