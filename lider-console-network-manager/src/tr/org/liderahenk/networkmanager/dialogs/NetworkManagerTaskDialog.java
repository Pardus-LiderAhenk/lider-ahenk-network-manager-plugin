package tr.org.liderahenk.networkmanager.dialogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.networkmanager.constants.NetworkManagerConstants;
import tr.org.liderahenk.networkmanager.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.dialogs.DefaultTaskDialog;
import tr.org.liderahenk.liderconsole.core.exceptions.ValidationException;
import tr.org.liderahenk.liderconsole.core.ldap.enums.DNType;
import tr.org.liderahenk.liderconsole.core.rest.requests.TaskRequest;
import tr.org.liderahenk.liderconsole.core.rest.utils.TaskRestUtils;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;
import tr.org.liderahenk.liderconsole.core.xmpp.notifications.TaskStatusNotification;

/**
 * 
 * @author <a href="mailto:mine.dogan@agem.com.tr">Mine Dogan</a>
 *
 */
public class NetworkManagerTaskDialog extends DefaultTaskDialog {
	
	private static final Logger logger = LoggerFactory.getLogger(NetworkManagerTaskDialog.class);
	
	private TabFolder tabFolder;
	
	private TableItem item;
	private TableViewer viewerDNS;
	private TableViewer viewerDomain;
	private TableViewer viewerHosts;
	private TableViewer viewerSettings;
	
	private Text txtInterfaces;
	private Text txtHost;
	private Text txtCurrentHostname;
	
	private List<String> columnTitles;
	
	private String dn;
	
	
	public NetworkManagerTaskDialog(Shell parentShell, String dn) {
		super(parentShell, dn);
		this.dn = dn;
		subscribeEventHandler(eventHandler);
		
		getData("GET_NETWORK_INFORMATION");
	}

	@Override
	public String createTitle() {
		return Messages.getString("NETWORK_MANAGEMENT");
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, Messages.getString("EXIT"), true);
	}
	
	private EventHandler eventHandler = new EventHandler() {
		@Override
		public void handleEvent(final Event event) {
			Job job = new Job("TASK") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("NETWORK_MANAGER", 100);
					try {
						TaskStatusNotification taskStatus = (TaskStatusNotification) event
								.getProperty("org.eclipse.e4.data");
						byte[] data = taskStatus.getResult().getResponseData();
						final Map<String, Object> responseData = new ObjectMapper().readValue(data, 0, data.length,
								new TypeReference<HashMap<String, Object>>() {
						});
						Display.getDefault().asyncExec(new Runnable() {

							@Override
							public void run() {
								if (responseData != null && !responseData.isEmpty()) {
									if (responseData.containsKey("interfaces") && responseData.containsKey("hosts")) {
										String interfaces = (String) responseData.get("interfaces");
										String hosts = (String) responseData.get("hosts");
										
										txtInterfaces.setText(interfaces);
										txtHost.setText(hosts);
										
										String[] lines = hosts.split("\n");
										
										for (String line : lines) {
											if (!line.isEmpty() && Character.isDigit(line.charAt(0))) {
												String[] hostnames = line.split("\\s+");
												
												TableItem item = new TableItem(viewerHosts.getTable(), SWT.NONE);
											    item.setText(0, hostnames[0]);
											    item.setText(1, hostnames[1]);
//											    item.setText(2, isActive); TODO is active?
											}
										}
										
										lines = interfaces.split("\n");
										for (String line : lines) {
											
											if (line.contains("iface")) {
												String[] items = line.split("\\s+");
												
												item = new TableItem(viewerSettings.getTable(), SWT.NONE);
												
//											    item.setText(1, isActive); TODO is active?
											    item.setText(2, items[1]);
											    item.setText(3, items[3]);
											}
											if (line.contains("address")) {
												String[] items = line.split(" ");
												
												item.setText(0, items[1]);
											}
										}
									}
									if (responseData.containsKey("dns")) {
										String dns = (String) responseData.get("dns");
										
										String[] lines = dns.split("\n");
										
										for (String line : lines) {
											if (line.contains("search")) {
												String[] domains = line.split("\\s+");
												for (int i = 1; i < domains.length; i++) {
													
													TableItem item = new TableItem(viewerDomain.getTable(), SWT.NONE);
												    item.setText(0, domains[i]);
												}
											}
											else if (line.contains("nameserver")) {
												String[] dnsHosts = line.split("\\s+");
												for (int i = 1; i < dnsHosts.length; i++) {
													
													TableItem item = new TableItem(viewerDNS.getTable(), SWT.NONE);
												    item.setText(0, dnsHosts[i]);
//												    item.setText(1, isActive); TODO is active?
												}
											}
										}
									}
									if (responseData.containsKey("machine_hostname")) {
										String machineHostname = (String) responseData.get("machine_hostname");
										
										txtCurrentHostname.setText(machineHostname);
									}
								}
							}
						});
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						Notifier.error("", Messages.getString("UNEXPECTED_ERROR_WHEN_GET_DATA"));
					}
					monitor.worked(100);
					monitor.done();

					return Status.OK_STATUS;
				}
			};

			job.setUser(true);
			job.schedule();
		}
	};
	
	public void getData(String commandId) {
		try {
			TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, getPluginName(),
					getPluginVersion(), commandId, null, null, null, new Date());
			TaskRestUtils.execute(task);
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
			Notifier.error(null, Messages.getString("ERROR_ON_EXECUTE"));
		}
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
		
		txtInterfaces = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		txtInterfaces.setLayoutData(data);
		txtInterfaces.setEditable(false);
		
		txtHost = new Text(group, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		txtHost.setLayoutData(data);
		txtHost.setEditable(false);
		
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
						"NEW_DNS", "ADD_DNS");
				dialog.create();
				dialog.open();
				
				viewerDNS.getTable().clearAll();
				viewerDNS.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
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
					logger.error(e1.getMessage(), e1);
				}
				 
				viewerDNS.getTable().clearAll();
				viewerDNS.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
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
						"NEW_DOMAIN", "ADD_DOMAIN");
				dialog.create();
				dialog.open();
				
				viewerDomain.getTable().clearAll();
				viewerDomain.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
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
					logger.error(e1.getMessage(), e1);
				}
				 
				viewerDomain.getTable().clearAll();
				viewerDomain.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
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
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.getString("HOSTS"));
		
		Group group = new Group(tabFolder, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		
		Button btnAdd = new Button(group, SWT.PUSH);
		btnAdd.setText(Messages.getString("ADD"));
		btnAdd.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/add.png")));
		btnAdd.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddHostDialog dialog = new AddHostDialog(Display.getDefault().getActiveShell(), dn);
				dialog.create();
				dialog.open();
				
				viewerHosts.getTable().clearAll();
				viewerHosts.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button btnDelete = new Button(group, SWT.PUSH);
		btnDelete.setText(Messages.getString("DELETE"));
		btnDelete.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/delete.png")));
		btnDelete.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = viewerHosts.getTable().getItem(viewerHosts.getTable().getSelectionIndex());
				
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(NetworkManagerConstants.PARAMETERS.IP, item.getText(0));
				parameterMap.put(NetworkManagerConstants.PARAMETERS.HOSTNAME, item.getText(1));
				parameterMap.put(NetworkManagerConstants.PARAMETERS.IS_ACTIVE, item.getText(2));
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, NetworkManagerConstants.PLUGIN_NAME,
						NetworkManagerConstants.PLUGIN_VERSION, "DELETE_HOST", parameterMap, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
				 
				viewerHosts.getTable().clearAll();
				viewerHosts.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewerHosts = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL
		        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		columnTitles = new ArrayList<String>();
		columnTitles.add("IP");
		columnTitles.add("HOSTNAME");
		columnTitles.add("ACTIVE");
		
		createColumns(group, viewerHosts);
		
		Table tableDomain = viewerHosts.getTable();
	    tableDomain.setHeaderVisible(true);
	    tableDomain.setLinesVisible(true);
	    
	    // define layout for the viewer
	    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
	    gridData.verticalAlignment = SWT.FILL;
	    gridData.horizontalAlignment = SWT.FILL;
	    viewerHosts.getControl().setLayoutData(gridData);
	    
	    tabItem.setControl(group);
		
	}
	
	public void createGeneralTab() {
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.getString("GENERAL"));
		
		Group group = new Group(tabFolder, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		
		Label lblCurrentHostname = new Label(group, SWT.NONE);
		lblCurrentHostname.setText(Messages.getString("CURRENT_HOSTNAME"));
		
		txtCurrentHostname = new Text(group, SWT.BORDER);
		txtCurrentHostname.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnChange = new Button(group, SWT.PUSH);
		btnChange.setText(Messages.getString("CHANGE"));
		btnChange.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/change.png")));
		btnChange.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(NetworkManagerConstants.PARAMETERS.HOSTNAME, txtCurrentHostname.getText());
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, NetworkManagerConstants.PLUGIN_NAME,
						NetworkManagerConstants.PLUGIN_VERSION, "CHANGE_HOSTNAME", parameterMap, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		tabItem.setControl(group);
	}
	
	public void createSettingsTab() {
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Messages.getString("NETWORK_SETTINGS"));
		
		Group group = new Group(tabFolder, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		
		Button btnAdd = new Button(group, SWT.PUSH);
		btnAdd.setText(Messages.getString("ADD"));
		btnAdd.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/add.png")));
		btnAdd.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddNetworkDialog dialog = new AddNetworkDialog(Display.getDefault().getActiveShell(), dn);
				dialog.create();
				dialog.open();
				
				viewerSettings.getTable().clearAll();
				viewerSettings.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button btnDelete = new Button(group, SWT.PUSH);
		btnDelete.setText(Messages.getString("DELETE"));
		btnDelete.setImage(new Image(Display.getCurrent(), this.getClass().getResourceAsStream("/icons/16/delete.png")));
		btnDelete.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = viewerSettings.getTable().getItem(viewerSettings.getTable().getSelectionIndex());
				
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put(NetworkManagerConstants.PARAMETERS.IP, item.getText(0));
				parameterMap.put(NetworkManagerConstants.PARAMETERS.IS_ACTIVE, item.getText(1));
				parameterMap.put(NetworkManagerConstants.PARAMETERS.NAME, item.getText(2));
				parameterMap.put(NetworkManagerConstants.PARAMETERS.TYPE, item.getText(3));
				
				TaskRequest task = new TaskRequest(new ArrayList<String>(getDnSet()), DNType.AHENK, NetworkManagerConstants.PLUGIN_NAME,
						NetworkManagerConstants.PLUGIN_VERSION, "DELETE_NETWORK", parameterMap, null, null, new Date());
				try {
					TaskRestUtils.execute(task);
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
				 
				viewerSettings.getTable().clearAll();
				viewerSettings.getTable().setItemCount(0);
				getData("GET_NETWORK_INFORMATION");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		viewerSettings = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL
		        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		
		columnTitles = new ArrayList<String>();
		columnTitles.add("IP");
		columnTitles.add("ACTIVE");
		columnTitles.add("NAME");
		columnTitles.add("TYPE");
		
		createColumns(group, viewerSettings);
		
		Table tableDomain = viewerSettings.getTable();
	    tableDomain.setHeaderVisible(true);
	    tableDomain.setLinesVisible(true);
	    
	    // define layout for the viewer
	    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
	    gridData.verticalAlignment = SWT.FILL;
	    gridData.horizontalAlignment = SWT.FILL;
	    viewerSettings.getControl().setLayoutData(gridData);
		
		tabItem.setControl(group);
		
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
	}
	
	@Override
	public Map<String, Object> getParameterMap() {
		return null;
	}

	@Override
	public String getCommandId() {
		return null;
	}

	@Override
	public String getPluginName() {
		return NetworkManagerConstants.PLUGIN_NAME;
	}

	@Override
	public String getPluginVersion() {
		return NetworkManagerConstants.PLUGIN_VERSION;
	}
	
}
