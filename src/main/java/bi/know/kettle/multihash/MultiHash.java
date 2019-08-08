package bi.know.kettle.multihash;

import org.apache.commons.codec.digest.DigestUtils;
import org.pentaho.di.core.exception.KettleException;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class MultiHash  extends BaseStep implements StepInterface {
	private static Class<?> PKG = MultiHash.class; // for i18n purposes, needed by Translator2!!
	
	private MultiHashMeta meta;
	private MultiHashData data;
	
	private int nbRows,nbFields;
	private String[] sourceFieldNames,stepFieldNames;
	private LinkedHashMap<String,int[]> outputField = new LinkedHashMap<>();
	private Object[] outputRow;
	private byte[] saltBytes;
	private int[] fieldIndex;
	private Boolean addSaltOutput;
	private String salt;
	
    public MultiHash(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
		super(s,stepDataInterface,c,t,dis);
	}
		
    public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
        meta = (MultiHashMeta) smi;
        data = (MultiHashData) sdi;

        Object[] r = getRow(); // get row, set busy!
        
        
        if (first){
            first = false;
            nbRows= 0;
            
	        data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
	        sourceFieldNames =  data.outputRowMeta.getFieldNames();
	        addSaltOutput = meta.getSaltOutput();
	        

   	        nbFields = data.outputRowMeta.size();
   	        saltBytes = environmentSubstitute(meta.getSalt()).getBytes();
   	        salt = environmentSubstitute(meta.getSalt());

   	        meta.getFields(data.outputRowMeta,getStepname(),null,null,this,repository,metaStore);


			for ( String key : meta.getOutputField().keySet() ) {
				data.outputRowMeta.addValueMeta(new ValueMetaString(environmentSubstitute(key)));
				stepFieldNames = meta.getFieldName(key);
				fieldIndex = new int[stepFieldNames.length];
				try {
					for(int i=0; i< stepFieldNames.length;i++) {
						fieldIndex[i] = data.outputRowMeta.indexOfValue(stepFieldNames[i]);
						if(fieldIndex[i]< 0){
							throw new KettleException("Unable to find field '" + stepFieldNames[i] + "' in input");
						}
					}
					outputField.put(environmentSubstitute(key),fieldIndex);
				}
				catch(Exception e) {
					stopStep(BaseMessages.getString(PKG, "MultiHash.generalError") + e.getMessage());
				}
			}
			if(meta.saltOutput){
				data.outputRowMeta.addValueMeta(new ValueMetaString(environmentSubstitute(meta.getSaltFieldName())));
			}
        }        
        
        if ( r == null ) {
          // no more input to be expected...
          setOutputDone();
          return false;
        }else
        {
        	try {
				Integer resultFieldIdx = nbFields;
				outputRow = r.clone();
				for ( String key : outputField.keySet()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					for (int i = 0; i < outputField.get(key).length; i++) {
						if(r[outputField.get(key)[i]]!=null) {
							baos.write(r[outputField.get(key)[i]].toString().getBytes());
						}
					}
					baos.write(saltBytes);

					byte[] byteArray = baos.toByteArray();

					//Integer resultFieldIdx = data.outputRowMeta.indexOfValue(key);

					//generate Hash
					if(byteArray.length!=0) {
						switch (meta.getHashType()) {
							case "CRC32":
								Long hash = generateLongHash(outputRow, key);
								outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, Long.toHexString(hash));
								break;
							case "SHA1":
								outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, DigestUtils.sha1Hex(byteArray));
								break;
							case "SHA256":
								outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, DigestUtils.sha256Hex(byteArray));
								break;
							case "SHA512":
								outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, DigestUtils.sha512Hex(byteArray));
								break;
							default:
								outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, environmentSubstitute(meta.getSalt()));
								break;
						}
					}else{
						outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, null);
					}
					resultFieldIdx++;
				}
				if(addSaltOutput){
					outputRow = RowDataUtil.addValueData(outputRow, resultFieldIdx, salt);
				}

				putRow(data.outputRowMeta, outputRow);
            	nbRows++;
       	     	setLinesWritten(nbRows);
       	     	setLinesOutput(nbRows);     
        		
        	}
        	catch(Exception e)
        	{
        		stopStep(BaseMessages.getString(PKG, "MultiHash.generalError") + e.getMessage());
        	}
        	
        	return true;
        }
    }
    
    private Long generateLongHash(Object[] r ,String key) {
    	try {
    		// get bytes from string
			String data = new String();

			for (int i = 0; i < outputField.get(key).length; i++) {
				if (r[outputField.get(key)[i]] != null) {
					data += r[outputField.get(key)[i]].toString();
				}
			}
        	data += environmentSubstitute(meta.getSalt());
            byte bytes[] = data.getBytes();
              
            Checksum hash = new CRC32();
             
            // update the current checksum with the specified array of bytes
            hash.update(bytes, 0, bytes.length);
            
    		return hash.getValue();
    		
    	}
    	catch(Exception e)
    	{
    		logError(BaseMessages.getString(PKG, "MultiHash.generalError") + e.getMessage());

    	}
    	return new Long(0);
	}

    private boolean stopStep(String errorMessage) {
    	logError(errorMessage);
   		setErrors(1);
   		stopAll();
   		setOutputDone();
    	return false;
    }
    
   public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
        meta = (MultiHashMeta) smi;
        data = (MultiHashData) sdi;


        if(Utils.isEmpty(getInputRowSets())){
			logError(BaseMessages.getString(PKG, "MultiHash.ErrorNoInputRows"));
			return false;
		}

        
        return super.init(smi, sdi);
    }
    
        
}
