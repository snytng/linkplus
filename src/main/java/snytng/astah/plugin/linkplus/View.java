package snytng.astah.plugin.linkplus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IDiagram;
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
		consoleHandler.setLevel(Level.CONFIG);
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
	JScrollPane textPanelScroll = null;
	JTextField prefixTextField = null;

	JSlider zoomSlider = null;


	private String[][] tabledata = {
			{"日本", "3勝", "0敗", "1分"},
			{"クロアチア", "3勝", "1敗", "0分"},
			{"ブラジル", "1勝", "2敗", "1分"},
			{"オーストラリア", "2勝", "2敗", "0分"}
			};

	private String[] columnNames = new String[]{"COUNTRY", "WIN", "LOST", "EVEN"};

	private DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

	public Container createPane() {
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

		scrollPanel = new JPanel();
		scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));
		textPanelScroll = new JScrollPane(
				scrollPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


		notesTable = new JTable(tableModel);
		notesTable.setAutoCreateRowSorter(true);
		notesTable.setColumnSelectionAllowed(true);
		notesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		notesTable.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseReleased(MouseEvent e) {
				// 選択行の行番号を取得します
				int row = notesTable.getSelectedRow();
				int col = notesTable.getSelectedColumn();

				System.out.println("行" + row + "::" + "列" + col);
		    }
		});

		notesTable.addMouseWheelListener(e -> {
			if(e.isControlDown()) {
				int wr = e.getWheelRotation();
				Font f = notesTable.getFont();
				int fs = f.getSize();
				if(wr > 0) {
					fs = Math.min(fs + wr, 100);
				} else if(wr < 0) {
					fs = Math.max(fs + wr, 10);
				}
				notesTable.setFont(new Font(f.getName(), f.getStyle(), fs));
			}
		});

		scrollPanel.add(notesTable);

		JPanel menuPanel = new JPanel();
		menuPanel.setLayout(new BorderLayout());

		JPanel prefixPanel = new JPanel();
		JLabel prefixLabel = new JLabel("接頭語");
		prefixTextField = new JTextField(10);
		prefixPanel.add(prefixLabel);
		prefixPanel.add(prefixTextField);

		zoomSlider = new JSlider(10, 100, 12);
		zoomSlider.setMajorTickSpacing(10);
		zoomSlider.setPaintTicks(true);
		zoomSlider.setSnapToTicks(true);
		zoomSlider.addChangeListener(e -> {
			int fs = zoomSlider.getValue();
			Font f = notesTable.getFont();
			notesTable.setFont(new Font(f.getName(), f.getStyle(), fs));
		});

		menuPanel.add(prefixPanel, BorderLayout.WEST);
		menuPanel.add(zoomSlider, BorderLayout.EAST);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(menuPanel, BorderLayout.NORTH);
		topPanel.add(buttonsPanel, BorderLayout.EAST);
		topPanel.add(textPanelScroll, BorderLayout.CENTER);

		return topPanel;
	}

	public IPresentation[] getPresentations() {
		IPresentation[] ps = diagramViewManager.getSelectedPresentations();
		if(ps.length == 0) {
			IDiagram d = diagramViewManager.getCurrentDiagram();
			if(d == null) {
				return new IPresentation[0];
			} else {
				try {
					return d.getPresentations();
				} catch (InvalidUsingException e) {
					return new IPresentation[0];
				}
			}
		} else {
			return ps;
		}
	}

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView() {
		try {
			logger.log(Level.INFO, "update diagram view.");

			tableModel.setRowCount(0);

			for(String[] td : tabledata) {
				tableModel.addRow(td);
			}

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
		updateDiagramView();
	}

	/**
	 * 要素の選択が変更されたら表示を更新する
	 */
	@Override
	public void entitySelectionChanged(IEntitySelectionEvent e) {
		logger.log(Level.INFO, "entitySelectionChanged");
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
