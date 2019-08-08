package bi.know.kettle.multihash;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.EnterStringDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

import java.util.LinkedHashMap;

public class MultiHashDialog extends BaseStepDialog implements StepDialogInterface {
	private static Class<?> PKG = MultiHashDialog.class; // for i18n purposes, needed by Translator2!!

	private MultiHashMeta input;
	
	//Buttons
	private Button wRemove;
	private Button wNew;
	private Button wGetFields;
	
	//fieldNames
	private String[]  fieldNames;
	
	//hashType
	private Label wlHashType;
	private CCombo wHashType;
	private FormData fdlHashType, fdHashType;
	
	//SaltField
	private Label wlSalt;
	private TextVar wSalt;
	private FormData fdlSalt, fdSalt;

	//Button Add salt to output
	private Label wlSaltOutput;
	private Button wSaltOutput;
	private FormData fdlSaltOutput, fdSaltOutput;
	
	//Salt Fieldname
	private Label wlSaltFieldName;
	private TextVar wSaltFieldName;
	private FormData fdlSaltFieldName, fdSaltFieldName;
	
	
	//FieldList
	private Label wlFields;
	private TableView wFields;
	private FormData fdlFields, fdFields;
	private ColumnInfo[] colHeader;
	
	//OutputFields
	private List wOutputList;
	private String selectedField;
	
	//listeners
	private Listener lsNew;
	private Listener lsRemove;
	private Listener lsGetFields;

	//temp store fieldlist
	private LinkedHashMap<String,String[]> outputFieldDialog = new LinkedHashMap<>();;
	

	public MultiHashDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
		super(parent, (BaseStepMeta) in, transMeta, sname);
		input = (MultiHashMeta) in;
	}

	
	@Override
	public String open() {
		Shell parent = getParent();
		Display display = parent.getDisplay();

		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook(shell);
		setShellImage(shell, input);
		
		//ModifyListener
		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				input.setChanged();
			}
		};
		changed = input.hasChanged();
		
		
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(BaseMessages.getString(PKG, "MultiHash.Shell.Title"));

		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;
		
		//add buttons
		//OK button
		wOK = new Button(shell, SWT.PUSH);
		wOK.setText(BaseMessages.getString(PKG, "System.Button.OK")); //$NON-NLS-1$
				
		//ok listener
		lsOK = new Listener() {
			public void handleEvent(Event e) {
				ok();
			}
		};
		wOK.addListener(SWT.Selection, lsOK);
		
		//new field button
		wNew = new Button(shell, SWT.PUSH);
		wNew.setText(BaseMessages.getString(PKG, "MultiHash.NewButton.Label")); //$NON-NLS-1$
		
		lsNew = new Listener() {
			public void handleEvent(Event e) {
				newField();
				input.setChanged();
				}
		};
		wNew.addListener(SWT.Selection, lsNew);
		
		//remove field button
		wRemove = new Button(shell, SWT.PUSH);
		wRemove.setText(BaseMessages.getString(PKG, "MultiHash.RemoveButton.Label")); //$NON-NLS-1$ 	
		
		lsRemove = new Listener() {
			public void handleEvent(Event e) {
				remove();
				input.setChanged();
			}

		};
		wRemove.addListener(SWT.Selection, lsRemove);

		//get fields button
		wGetFields = new Button(shell, SWT.PUSH);
		wGetFields.setText(BaseMessages.getString(PKG, "MultiHash.GetFieldsButton.Label")); //$NON-NLS-1$

		lsGetFields = new Listener() {
			public void handleEvent(Event e) {
				getFields();
				input.setChanged();
			}
		};
		wGetFields.addListener(SWT.Selection, lsGetFields);

				
		//cancel button
		wCancel = new Button(shell, SWT.PUSH);
		wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel")); //$NON-NLS-1$
		
		//cancel listener
				lsCancel = new Listener() {
					public void handleEvent(Event e) {
						cancel();
					}
				};
				wCancel.addListener(SWT.Selection, lsCancel);

		BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK,wNew,wRemove,wGetFields, wCancel }, margin, null);

		//stepName label
		//create label
		wlStepname = new Label(shell, SWT.RIGHT);
		wlStepname.setText(BaseMessages.getString(PKG,"MultiHash.Stepname.Label"));
		props.setLook(wlStepname);
		//create form location
		fdlStepname=new FormData();
		fdlStepname.left = new FormAttachment(0,0);
		fdlStepname.right = new FormAttachment(middle,-margin);
		fdlStepname.top = new FormAttachment(0,margin);
		//Attach to form
		wlStepname.setLayoutData(fdlStepname);
		
		//stepName Textbox
		//create textbox
		wStepname = new Text(shell,SWT.SINGLE|SWT.LEFT|SWT.BORDER);
		wStepname.setText(stepname);
		props.setLook(wStepname);
		//add listener
		wStepname.addModifyListener(lsMod);
		//create form location
		fdStepname=new FormData();
		fdStepname.left = new FormAttachment(middle, 0);
		fdStepname.top  = new FormAttachment(0, margin);
		fdStepname.right= new FormAttachment(100, 0);
		//attach to form
		wStepname.setLayoutData(fdStepname);
		
		//Add Type Field
		wlHashType = new Label( shell, SWT.RIGHT );
	    wlHashType.setText( BaseMessages.getString( PKG, "MultiHash.Type.Label" ) );
	    props.setLook( wlHashType );
	    fdlHashType = new FormData();
	    fdlHashType.left = new FormAttachment( 0, 0 );
	    fdlHashType.right = new FormAttachment( middle, -margin );
	    fdlHashType.top = new FormAttachment( wStepname, margin );
	    wlHashType.setLayoutData( fdlHashType );
	    
	    wHashType = new CCombo( shell, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER );
	    
	    for ( int i = 0; i < MultiHashMeta.getHashtypeCodes().length; i++ ) {
	    	wHashType.add( MultiHashMeta.getHashtypeDescs()[i] );
	      }
	    
	    props.setLook( wHashType );
	    wHashType.select(0);
	    wHashType.addModifyListener(lsMod);
	    
	    fdHashType = new FormData();
	    fdHashType.left = new FormAttachment( middle, 0 );
	    fdHashType.top = new FormAttachment( wStepname, margin );
	    fdHashType.right = new FormAttachment( 100, 0 );
	    wHashType.setLayoutData( fdHashType );
	    

	    
	    //Salt Field
	    wlSalt = new Label(shell, SWT.RIGHT);
	    wlSalt.setText(BaseMessages.getString( PKG, "MultiHash.Salt.Label") );
	    props.setLook(wlSalt);
	    fdlSalt = new FormData();
	    fdlSalt.left = new FormAttachment( 0, 0 );
	    fdlSalt.right = new FormAttachment( middle, -margin );
	    fdlSalt.top = new FormAttachment( wHashType, margin );
	    wlSalt.setLayoutData(fdlSalt);
	    
	    wSalt = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		props.setLook(wSalt);
		wSalt.addModifyListener( lsMod );
		
		fdSalt = new FormData();
		fdSalt.left = new FormAttachment( middle, 0 );
		fdSalt.top = new FormAttachment( wHashType, margin );
		fdSalt.right = new FormAttachment( 100, 0 );
	    wSalt.setLayoutData( fdSalt );

	    //salt output checkbox
		wlSaltOutput = new Label(shell, SWT.RIGHT);
		wlSaltOutput.setText(BaseMessages.getString( PKG, "MultiHash.SaltOutput.Label") );
		props.setLook(wlSaltOutput);

		fdlSaltOutput = new FormData();
		fdlSaltOutput.left = new FormAttachment( 0, 0 );
		fdlSaltOutput.top = new FormAttachment( wSalt, margin );
		fdlSaltOutput.right = new FormAttachment( middle, -margin );
		wlSaltOutput.setLayoutData( fdlSaltOutput );
		wSaltOutput = new Button( shell, SWT.CHECK );
		//wCompatibility.setToolTipText( BaseMessages.getString( PKG, "CheckSumDialog.CompatibilityMode.Tooltip" ) );
		props.setLook( wSaltOutput );
		fdSaltOutput= new FormData();
		fdSaltOutput.left = new FormAttachment( middle, 0 );
		fdSaltOutput.top = new FormAttachment( wSalt, margin );
		fdSaltOutput.right = new FormAttachment( 100, 0 );
		wSaltOutput.setLayoutData( fdSaltOutput );
		wSaltOutput.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected( SelectionEvent event ) {
				wSaltFieldName.setEnabled(wSaltOutput.getSelection());
			}
		} );


	    //salt Output field
		wlSaltFieldName = new Label(shell, SWT.RIGHT);
		wlSaltFieldName.setText(BaseMessages.getString( PKG, "MultiHash.SaltFieldName.Label") );
		props.setLook(wlSaltFieldName);
		fdlSaltFieldName = new FormData();
		fdlSaltFieldName.left = new FormAttachment( 0, 0 );
		fdlSaltFieldName.right = new FormAttachment( middle, -margin );
		fdlSaltFieldName.top = new FormAttachment( wSaltOutput, margin );
		wlSaltFieldName.setLayoutData(fdlSaltFieldName);

		wSaltFieldName = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		props.setLook(wSaltFieldName);
		wSaltFieldName.addModifyListener( lsMod );

		fdSaltFieldName = new FormData();
		fdSaltFieldName.left = new FormAttachment( middle, 0 );
		fdSaltFieldName.top = new FormAttachment( wSaltOutput, margin );
		fdSaltFieldName.right = new FormAttachment( 100, 0 );
		wSaltFieldName.setLayoutData( fdSaltFieldName );
	    
	    //Fieldlist
	    wlFields = new Label(shell, SWT.NONE);
	    wlFields.setText(BaseMessages.getString( PKG, "MultiHash.Fields.Label") );
	    props.setLook(wlFields);
	    fdlFields = new FormData();
	    fdlFields.left= new FormAttachment(middle,margin);
	    fdlFields.right = new FormAttachment( middle, -margin );
	    fdlFields.top = new FormAttachment(wSaltFieldName,margin);
	    wlFields.setLayoutData(fdlFields);
	    
	    //Get Fieldnames    
	    RowMetaInterface prevFields = null;
		try {
			prevFields = transMeta.getPrevStepFields( stepname );
			fieldNames = prevFields.getFieldNames();
		} catch (KettleStepException e) {
			logError( BaseMessages.getString( PKG, "MultiHash.ErroGettingFields" ) );
			fieldNames = new String[] {};
		}
	    
	    //set columns for Form
	    final int FieldsCols = 1;
	    final int FieldRows = 1;

	    colHeader = new ColumnInfo[FieldsCols];
	    colHeader[0]= new ColumnInfo(BaseMessages.getString( PKG, "MultiHash.Fields.Label"), ColumnInfo.COLUMN_TYPE_CCOMBO, fieldNames );
	    wFields = new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colHeader, FieldRows, false, lsMod, props);
	    
	    fdFields = new FormData();
	    fdFields.left = new FormAttachment(middle,margin);
	    fdFields.right = new FormAttachment( 100, 0 );
	    fdFields.top = new FormAttachment(wlFields,margin);
	    fdFields.bottom = new FormAttachment( wOK, -4 * margin );
	    wFields.setLayoutData( fdFields );
	    
	    
	    //Output fields List
	    
	    Label wlOutputFieldList = new Label( shell, SWT.LEFT );
	    wlOutputFieldList.setText( BaseMessages.getString( PKG, "MultiHash.OutputField.Label" ) );
	    props.setLook( wlOutputFieldList );
	    FormData fdlOutputFieldList = new FormData();
		fdlOutputFieldList.left = new FormAttachment( 0, 0 );
	    fdlOutputFieldList.right = new FormAttachment( middle, -margin );
	    fdlOutputFieldList.top = new FormAttachment( wlFields, margin );
	    wlOutputFieldList.setLayoutData( fdlOutputFieldList );
	    wOutputList = new List( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
	    
	    props.setLook( wOutputList );
	    wOutputList.addSelectionListener( new SelectionAdapter() {
	      public void widgetSelected( SelectionEvent event ) {
	        showSelectedFields( wOutputList.getSelection()[0] );
	      }
	    } );

	    FormData fdOutputFieldList = new FormData();
	    fdOutputFieldList.left = new FormAttachment( 0, 0 );
	    fdOutputFieldList.top = new FormAttachment( wlOutputFieldList, margin );
	    fdOutputFieldList.right = new FormAttachment( middle, -margin );
	    fdOutputFieldList.bottom = new FormAttachment( wOK, -4 *margin);
	    wOutputList.setLayoutData( fdOutputFieldList );
	    

		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener( new ShellAdapter() { public void
		shellClosed(ShellEvent e) { cancel(); } } );
		
		//set focus on stepname button
	    wStepname.selectAll();
	    wStepname.setFocus();
	    
	    //do OK when pressing enter after changing stepname
	    lsDef = new SelectionAdapter() {
	        public void widgetDefaultSelected( SelectionEvent e ) {
	          ok();
	        }
	      };
	      wStepname.addSelectionListener( lsDef );
		

		// Set the shell size, based upon previous time...
		setSize();

	
		getData();

		input.setChanged(false);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return stepname;
	
	}
	
	
	
	//get values from meta
	private void getData(){
		wSaltFieldName.setEnabled(false);
		//get salt from meta
		if(input.getSalt() != null){
			wSalt.setText(input.getSalt());
		}else {
			wSalt.setText("");
		}

		//get Hash from meta
		wHashType.select(input.getHashTypeSelection());

		//get outputfields from meta
		outputFieldDialog = input.getOutputField();


		//populate OutputFieldList		
		if(outputFieldDialog.size() > 0) {

			//set defalt selection
			for ( String key : outputFieldDialog.keySet() ) {
			    wOutputList.add(key);
			}
			wOutputList.setSelection(0);
			selectedField = wOutputList.getItem(0).toString();
		}else {
			selectedField=null;
			wlFields.setEnabled(false);
			wFields.setEnabled(false);
		}


		wSaltOutput.setSelection(input.getSaltOutput());
		//enable/disable inputfield
		wSaltFieldName.setEnabled(input.getSaltOutput());


		wSaltFieldName.setText(input.getSaltFieldName());
		
		populateTable();
		
	}
	
	private void showSelectedFields(String selection) {
		saveFields();
		selectedField = selection;
		populateTable();
		
	}
	
	//cancel function
	private void cancel() {
		stepname = null;
		input.setChanged(changed);
		dispose();
	}
	
	//ok function
	private void ok() {
		saveFields();
		stepname = wStepname.getText();
		input.setSalt(wSalt.getText());
		input.setHashType(wHashType.getText());
		input.setOutputField(outputFieldDialog);
		input.setSaltOutput(wSaltOutput.getSelection());
		input.setSaltFieldName(wSaltFieldName.getText());
		dispose();
	}
	
	public void newField() {
		saveFields();
		EnterStringDialog enterStringDialog = new EnterStringDialog(shell, "",
				BaseMessages.getString(PKG, "MultiHash.NewField.Title"),
				BaseMessages.getString(PKG, "MultiHash.NewField.Message"));
		String fieldName = enterStringDialog.open();
		if (fieldName != null) {
			if ( !fieldName.isEmpty()) {
				if (outputFieldDialog.containsKey(fieldName)) {
					MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
					//CHECKSTYLE:LineLength:OFF
					messageBox.setText(BaseMessages.getString(PKG, "MultiHash.FieldExistsMessageBox.Title"));
					messageBox.setMessage(BaseMessages.getString(PKG, "MultiHash.FieldExistsMessageBox.Message"));
					messageBox.open();
					return;
				}
				//add new field to hashmap
				outputFieldDialog.put(fieldName, new String[0]);
				//input.allocate(fieldName,0);
				int items = wOutputList.getItemCount();
				wOutputList.add(fieldName);
				wOutputList.setSelection(items);
				selectedField = fieldName;
				populateTable();
			} else {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
				//CHECKSTYLE:LineLength:OFF
				messageBox.setText(BaseMessages.getString(PKG, "MultiHash.FieldEmptyMessageBox.Title"));
				messageBox.setMessage(BaseMessages.getString(PKG, "MultiHash.FieldEmptyMessageBox.Message"));
				messageBox.open();
				return;
			}
		}
	}
	
	public void remove() {
		try{
			if(outputFieldDialog.containsKey(selectedField)){
				outputFieldDialog.remove(selectedField);
				wOutputList.remove(selectedField);
				int items = wOutputList.getItemCount();
				if(items > 0){
					wOutputList.setSelection(0);
					selectedField = wOutputList.getItem(0).toString();
				} else {
					selectedField=null;
					wlFields.setEnabled(false);
					wFields.setEnabled(false);
				}
				populateTable();
			}else{
				throw new KettleException("No field Selected to remove");
			}
		}catch (Exception e){
			logError(BaseMessages.getString(PKG, "MultiHash.generalError") + e.getMessage());
		}
	}
	
	
	
	private void populateTable() {

		Table fieldsTable = wFields.table;

		fieldsTable.removeAll();
		if(outputFieldDialog.size() > 0){
			wlFields.setEnabled( true );
			wFields.setEnabled( true );
			if (outputFieldDialog.get(selectedField).length > 0) {
				for (int i = 0; i < outputFieldDialog.get(selectedField).length; i++) {
					TableItem ti = new TableItem(fieldsTable, SWT.NONE);
					ti.setText(0, "" + (i + 1));
					ti.setText(1, outputFieldDialog.get(selectedField)[i]);
				}
			} else {
				TableItem ti = new TableItem(fieldsTable, SWT.NONE);
				ti.setText(0, "1");
				ti.setText(1, "");
			}
		} else {
			TableItem ti = new TableItem(fieldsTable, SWT.NONE);
			ti.setText(0, "1");
			ti.setText(1, "");
		}

		wFields.setRowNums();
		wFields.optWidth(true);

	}

	private void saveFields(){
		//Write fieldlist to metadata
		try {
			if(selectedField == ""){
				throw new KettleException("No field Selected to save fields for");
			}

			int nrfields = wFields.nrNonEmpty();
			String[] fieldList = new String[nrfields];

			for(int i=0; i < nrfields; i++) {
				TableItem ti = wFields.getNonEmpty(i);
				fieldList[i] = ti.getText(1);
			}
			if(nrfields > 0) {
				outputFieldDialog.put(selectedField, fieldList);
			}

		}catch (Exception e) {
			logError(BaseMessages.getString(PKG, "MultiHash.generalError") + e.getMessage());
		}
	}

	private void getFields(){
		//add all fields to fieldlist
		try {
			//get input row meta
			RowMetaInterface r = transMeta.getPrevStepFields( stepname );
			if(r != null) {
				if (wFields.isEnabled()) {
					TableItemInsertListener insertListener = new TableItemInsertListener() {
						@Override
						public boolean tableItemInserted(TableItem tableItem, ValueMetaInterface v) {
							tableItem.setText(2, BaseMessages.getString(PKG, "System.Combo.Yes"));
							return true;
						}
					};
					BaseStepDialog
							.getFieldsFromPrevious(r, wFields, 1, new int[]{1}, new int[]{}, -1, -1, insertListener);
				}
			}

		} catch (KettleException e){
			new ErrorDialog( shell, BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Title" ), BaseMessages
					.getString( PKG, "System.Dialog.GetFieldsFailed.Message" ), e );
		}

	}



}