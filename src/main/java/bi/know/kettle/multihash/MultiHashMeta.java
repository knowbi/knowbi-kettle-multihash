package bi.know.kettle.multihash;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import groovy.ui.Console;
import jxl.biff.drawing.Origin;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

@Step(id = "MultiHash", 
image = "MultiHash.svg",
i18nPackageName="bi.know.kettle.multihash",
name="MultiHash.Step.Name",
description = "MultiHash.Step.Description",
categoryDescription="MultiHash.Step.Category",
isSeparateClassLoaderNeeded=true
)
public class MultiHashMeta extends BaseStepMeta implements StepMetaInterface{
	private static Class<?> PKG = MultiHash.class; // for i18n purposes, needed by Translator2!!
	
	public String salt, hashType, resultField;
	public String[] fieldName ;
	public LinkedHashMap<String,String[]> outputField = new LinkedHashMap<String,String[]>();

	//lsit of Hash types
	public static String[] HashtypeCodes = { "CRC32", "SHA1","SHA256","SHA512"};
	public static String[] HashtypeDescs = {
			BaseMessages.getString( PKG, "MultiHash.Type.CRC32" ),
			BaseMessages.getString( PKG, "MultiHash.Type.SHA1" ),
			BaseMessages.getString( PKG, "MultiHash.Type.SHA256" ),
			BaseMessages.getString( PKG, "MultiHash.Type.SHA512" )
			};
	
	
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
		return new MultiHash(stepMeta,stepDataInterface,cnr,transMeta,disp);
	}
	
	public StepDataInterface getStepData() {
		return new MultiHashData();
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name){
		return new MultiHashDialog(shell, meta, transMeta, name);
	}
	
	public void setDefault() {
		
	}
	

	public String getXML() throws KettleException{
		String retval = "";
		retval += "<hashType>" + hashType + "</hashType>"  + Const.CR;
		retval += "<salt>" + salt + "</salt>"  + Const.CR;
		retval += "<fields>"  + Const.CR;
		for ( String key : outputField.keySet() ) {
			retval += "<field>" + Const.CR;
			retval += "<name>" + key + "</name>"  + Const.CR;
			String[] inputFields  = outputField.get(key);
			for(int i = 0; i < inputFields.length; i++){
				retval += "<inputField>"+ inputFields[i] + "</inputField>";
			}
			retval += "</field>" + Const.CR;
		}
		retval += "</fields>" + Const.CR;
		return retval;
	}
	
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String,Counter> counters) throws KettleXMLException{		
		salt = XMLHandler.getTagValue(stepnode, "salt");
		hashType = XMLHandler.getTagValue(stepnode, "hashType");
		
		Node fields = XMLHandler.getSubNode(stepnode, "fields");
		int nrfields = XMLHandler.countNodes(fields, "field");
		
		for(int i =0;i< nrfields;i++) {
			Node field = XMLHandler.getSubNodeByNr(fields, "field", i);

			resultField = XMLHandler.getTagValue(field, "name");
			if (resultField != null) {
				int nrInputFields = XMLHandler.countNodes(field, "inputField");

				allocate(resultField, nrInputFields);
				String[] inputFields = new String[nrInputFields];

				for (int ii = 0; ii < nrInputFields; ii++) {
					Node inputField = XMLHandler.getSubNodeByNr(field, "inputField", ii);
					inputFields[ii] = inputField.getTextContent();
				}

				outputField.put(resultField, inputFields);

			}
		}

	}

	
	public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		if (prev==null || prev.size()==0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Step is connected to previous one, receiving "+prev.size()+" fields", stepMeta);
			remarks.add(cr);
		}
		
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Step is receiving info from other steps.", stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "No input received from other steps!", stepMeta);
			remarks.add(cr);
		}
		/*if(Utils.isEmpty(resultField)) {
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "No resultfield specified!", stepMeta);
			remarks.add(cr);
		}*/
	}
	
	public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space){
		if(outputField.keySet().size() > 0){
			for(String key : outputField.keySet()){
				ValueMetaInterface v = null;
				v = new ValueMetaString( space.environmentSubstitute( key ) );
				v.setOrigin(origin);
				r.addValueMeta(v);
			}
		}
	}
		
	public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String,Counter> counters) throws KettleException{
		//TODO: add readRep
		hashType = rep.getStepAttributeString(id_step,"hashType");
		salt = rep.getStepAttributeString(id_step,"salt");
		int nrFields = rep.countNrStepAttributes(id_step,"field");

		//read outputfields combined with inputfields
		for(int i=0;i<nrFields;i++){
			String field = rep.getStepAttributeString(id_step,i,"field");
			String key = field.split(":",2)[0];
			String[] fields = field.split(":",2)[1].replaceFirst("\\[\\[","").replaceAll("\\]\\]$","").split("\\],\\[");
			System.out.println(key.toString());
			System.out.println(Arrays.asList(fields));
			outputField.put(key,fields);
		}

	}

	public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException{
		//TODO: add saveRep
		rep.saveStepAttribute(id_transformation,id_step,"hashType",hashType);
		rep.saveStepAttribute(id_transformation,id_step,"salt",salt);

		//create list for outputfiedls combined with inputfields
		int keyIndex=0;
		for(String key : outputField.keySet()){
			String field = key + ":" + "[[";
			int len = outputField.get(key).length;
			len = len -1;
			for(int i=0;i<len;i++){
				field += outputField.get(key)[i] + "],[";
			}
			field += outputField.get(key)[len] + "]]";
			rep.saveStepAttribute(id_transformation,id_step,keyIndex,"field", field);
			keyIndex++;
		}
	}
	
	public Object clone()
	{
		Object retval = super.clone();
		return retval;
	}
	
	
	public void allocate(String outputFieldName , int nrfields) {
		fieldName = new String[nrfields];
		this.outputField.put(outputFieldName,fieldName);
	}
	
	//Getters and Setters
	
	public String getSalt() {
		if(salt == null)
		{
			salt="";
		}
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}
	
	public String getResultField() {
		return resultField;
	}

	public void setResultField(String resultField) {
		this.resultField = resultField;
	}
	
	public String getHashType() {
		return hashType;
	}

	
	public void setHashType(String hashType) {
		//translate selector description to code for XML
		for(int i=0 ; i<HashtypeDescs.length;i++) {
			if(hashType.equals(HashtypeDescs[i])) {
				this.hashType = HashtypeCodes[i];
			}
		}
	}
	
	public static String[] getHashtypeCodes() {
		return HashtypeCodes;
	}

	public static String[] getHashtypeDescs() {
		return HashtypeDescs;
	}
	
	//get index for selector
	public int getHashTypeSelection() {
		if(hashType == null) {
			return 0;
		}
		for(int i=0 ; i<HashtypeCodes.length;i++) {
			if(hashType.equals(HashtypeCodes[i])) {
				return i;
			}
		}
		return 0;
		}
	
	public String[] getFieldName(String outputFieldName) {
		if(outputField.get(outputFieldName) == null) {
			return new String[0];
		}else
		{
			return outputField.get(outputFieldName);
		}			
	}

	public void setFieldName(String outputFieldName, String[] fields) {
		this.outputField.put(outputFieldName,fields);
	}
	
	public LinkedHashMap<String, String[]> getOutputField() {
		LinkedHashMap<String, String[]> copy = new LinkedHashMap<String, String[]>();
		for (Map.Entry<String, String[]> entry : outputField.entrySet())
		{
			copy.put(entry.getKey(),
					entry.getValue());
		}
		return copy;
	}

	public void setOutputField(LinkedHashMap<String, String[]> outputField) {
		this.outputField = outputField;
	}
	
	public void removeOutputField(String outputField) {
		this.outputField.remove(outputField);
	}
}