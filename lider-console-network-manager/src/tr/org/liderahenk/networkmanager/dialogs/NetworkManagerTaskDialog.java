package tr.org.liderahenk.networkmanager.dialogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import tr.org.liderahenk.networkmanager.constants.NetworkManagerConstants;
import tr.org.liderahenk.networkmanager.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.dialogs.DefaultTaskDialog;
import tr.org.liderahenk.liderconsole.core.exceptions.ValidationException;
import tr.org.liderahenk.liderconsole.core.ldap.enums.DNType;
import tr.org.liderahenk.liderconsole.core.rest.requests.TaskRequest;
import tr.org.liderahenk.liderconsole.core.rest.utils.TaskRestUtils;

/**
 * 
 * @author <a href="mailto:mine.dogan@agem.com.tr">Mine Dogan</a>
 *
 */
public class NetworkManagerTaskDialog extends DefaultTaskDialog {
	
	private TabFolder tabFolder;
	
	private TableViewer viewerDNS;
	private TableItem itemDNS;
	private TableViewer viewerDomain;
	private TableItem itemDomain;
	
	private List<String> columnTitles;
	
	private String dn;
	
	
	public NetworkManagerTaskDialog(Shell parentShell, String dn) {
		super(parentShell, dn);
		this.dn = dn;
	}

	@Override
	public String createTitle() {
		return Messages.getString("NETWORK_MANAGEMENT");
	}

	@Override
	public Control createTaskDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

		tabFolder = new TabFolder(composite, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createConfigurationTab();
		createDNSTab();
		createHostTab();
		createGeneralTab();
		createSettingsTab();
		
		return null;
	}
	
	public void createConfigurationTab() {
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.getString("CURRENT_CONFIGURATION"));
		
		Group group = new Group(tabFolder, SWT.NONE);
		group.setLayout(new GridLayout(2, true));
		
		Label lblInterfaces = new Label(group, SWT.NONE);
		lblInterfaces.setText(Messages.getString("NETWORK_INTERFACES"));
		
		Label lblHost = new Label(group, SWT.NONE);
		lblHost.setText(Messages.getString("HOSTS"));
		
		GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
		data.widthHint = 300;
		data.heightHint = 180;
		
		Text txtInterfaces = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		txtInterfaces.setLayoutData(data);
		
		Text txtHost = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		txtHost.setLayoutData(data);
		
		tabItem.setControl(group);
		
	}
	
	public void createDNSTab() {
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.getString("DNS"));
		
		Group group = new Group(tabFolder, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		
		Button btnAddDNS = new Button(group, SWT.PUSH);
		btnAddDNS.setText(Messages.getString("ADD"));
		btnAddDNS.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/add.png")));
		btnAddDNS.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddDNSDialog dialog = new AddDNSDialog(Display.getDefault().getActiveShell(), dn, 
						"NEW_DNS", "SET_DNS");
				dialog.create();
				dialog.open();
				
				viewerDNS.getTable().clearAll();
				viewerDNS.getTable().setItemCount(0);
//				getDNSData();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button btnDeleteDNS = new Button(group, SWT.PUSH);
		btnDeleteDNS.setText(Messages.getString("DELETE"));
		btnDeleteDNS.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/delete.png")));
		btnDeleteDNS.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = viewerDNS.getTable().getItem(viewerDNS.getTable().getSelectionIndex());
				
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(NetworkManagerConstants.PARAMETERS.IP, item.getText(0));
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, NetworkManagerConstants.PLUGIN_NAME,
						NetworkManagerConstants.PLUGIN_VERSION, "DELETE_DNS", parameterMap, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				 
				viewerDNS.getTable().clearAll();
				viewerDNS.getTable().setItemCount(0);
//				getDNSData();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewerDNS = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL
		        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		columnTitles = new ArrayList<String>();
		columnTitles.add("IP");
		columnTitles.add("ACTIVE");
		
		createColumns(group, viewerDNS);
		
		Table table = viewerDNS.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    
	    // define layout for the viewer
	    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
	    gridData.verticalAlignment = SWT.FILL;
	    gridData.horizontalAlignment = SWT.FILL;
	    viewerDNS.getControl().setLayoutData(gridData);
	    
	    Label separator = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
	    
	    Button btnAddDomain = new Button(group, SWT.PUSH);
		btnAddDomain.setText(Messages.getString("ADD"));
		btnAddDomain.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/add.png")));
		btnAddDomain.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddDNSDialog dialog = new AddDNSDialog(Display.getDefault().getActiveShell(), dn, 
						"NEW_DOMAIN", "SET_DOMAIN");
				dialog.create();
				dialog.open();
				
				viewerDomain.getTable().clearAll();
				viewerDomain.getTable().setItemCount(0);
//				getDomainData();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button btnDeleteDomain = new Button(group, SWT.PUSH);
		btnDeleteDomain.setText(Messages.getString("DELETE"));
		btnDeleteDomain.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/delete.png")));
		btnDeleteDomain.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = viewerDomain.getTable().getItem(viewerDomain.getTable().getSelectionIndex());
				
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(NetworkManagerConstants.PARAMETERS.DOMAIN, item.getText(0));
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, NetworkManagerConstants.PLUGIN_NAME,
						NetworkManagerConstants.PLUGIN_VERSION, "DELETE_DOMAIN", parameterMap, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				 
				viewerDomain.getTable().clearAll();
				viewerDomain.getTable().setItemCount(0);
//				getDomainData();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewerDomain = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL
		        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		columnTitles = new ArrayList<String>();
		columnTitles.add("DOMAIN");
		
		createColumns(group, viewerDomain);
		
		Table tableDomain = viewerDomain.getTable();
	    tableDomain.setHeaderVisible(true);
	    tableDomain.setLinesVisible(true);
	    
	    viewerDomain.getControl().setLayoutData(gridData);
	    
	    tabItem.setControl(group);
		
	}
	
	public void createHostTab() {
		
	}
	
	public void createGeneralTab() {
		
	}
	
	public void createSettingsTab() {
		
	}
	
	// create the columns for the table
	private void createColumns(final Composite parent, final TableViewer viewer) {
		int[] bounds = { 120, 120, 120, 120 };

		for (int i = 0; i < columnTitles.size(); i++) {
			createTableViewerColumn(viewer, Messages.getString(columnTitles.get(i)), bounds[i], i);
		}
	}
	
	private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int bound, final int colNumber) {
		 final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
			        SWT.NONE);
		 final TableColumn column = viewerColumn.getColumn();
		 column.setText(title);
		 column.setWidth(bound);
		 column.setResizable(true);
		 column.setMoveable(true);
		 return viewerColumn;
	}

	@Override
	public void validateBeforeExecution() throws ValidationException {
		// TODO triggered before task execution
	}
	
	@Override
	public Map<String, Object> getParameterMap() {
		// TODO custom parameter map
		return new HashMap<String, Object>();
	}

	@Override
	public String getCommandId() {
		// TODO command id which is used to match tasks with ICommand class in the corresponding Lider plugin
		return "SAMPLE_COMMAND1";
	}

	@Override
	public String getPluginName() {
		return "network-manager";
	}

	@Override
	public String getPluginVersion() {
		return "1.0.0";
	}
	
}
