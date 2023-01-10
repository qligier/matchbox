package ch.ahdis.matchbox;

import java.lang.reflect.Field;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.OperationDefinition.OperationDefinitionParameterComponent;
import org.hl7.fhir.r4.model.OperationDefinition.OperationParameterUse;
import ch.ahdis.matchbox.CliContext;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ch.ahdis.fhir.hapi.jpa.validation.ValidationProvider;

public class MatchboxCapabilityStatementProvider extends ServerCapabilityStatementProvider {

	
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MatchboxCapabilityStatementProvider.class);
	private StructureDefinitionResourceProvider structureDefinitionProvider;

	public MatchboxCapabilityStatementProvider(RestfulServer theServerConfiguration, StructureDefinitionResourceProvider structureDefinitionProvider) {
		super(theServerConfiguration, null, null);
		this.structureDefinitionProvider = structureDefinitionProvider;
		this.setPublisher("matchbox-v3");
	}
	
	@Read(typeName = "OperationDefinition")
	@Override
	public IBaseResource readOperationDefinition(@IdParam IIdType theId, RequestDetails theRequestDetails) {
		
		org.hl7.fhir.r4.model.OperationDefinition operationDefintion = (org.hl7.fhir.r4.model.OperationDefinition) super.readOperationDefinition(theId, theRequestDetails);
		if ("Validate".equals(operationDefintion.getName())) {
			ourLog.info("adding profiles to $validate");
//	existing:	  "parameter": [
//		                {
//		                  "name": "return",
//		                  "use": "out",
//		                  "min": 1,
//		                  "max": "1"
//		                }
//		              ]
			OperationDefinitionParameterComponent parameter = operationDefintion.addParameter();
			parameter.setName("resource").setUse(OperationParameterUse.IN).setMin(0).setMax("1").setType("Resource");
			parameter = operationDefintion.addParameter();
			parameter.setName("mode").setUse(OperationParameterUse.IN).setMin(0).setMax("1").setType("code");
			parameter = operationDefintion.addParameter();
			parameter.setName("profile").setUse(OperationParameterUse.IN).setMin(0).setMax("1").setType("canonical");
			parameter.setTargetProfile(structureDefinitionProvider.getCanonicals());
			parameter.setName("reload").setUse(OperationParameterUse.IN).setMin(0).setMax("1").setType("boolean");

			CliContext cliContext = new CliContext();
			
			List<Field> cliContextProperties = ValidationProvider.getValidateEngineParameters(cliContext);
			for (Field field : cliContextProperties) {
				parameter = operationDefintion.addParameter();
				parameter.setName(field.getName()).setUse(OperationParameterUse.IN).setMin(0).setMax("1").setType(field.getType().equals(Boolean.class) ? "boolean" : "string");
			}
			parameter = operationDefintion.addParameter();
			parameter.setTargetProfile(structureDefinitionProvider.getCanonicals());
		}
		return operationDefintion;
	}


}
