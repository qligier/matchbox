package ch.ahdis.validation;

import static org.junit.Assert.assertEquals;

/**
 * Attention: if it is the first test run, an error about not connecting to port 8080 appears, running IgValidateR4 first works 
 */
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.Application;



@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = {Application.class})
@ActiveProfiles("test1")
public class IgValidateRawProfileTest {


  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IgValidateRawProfileTest.class);

  private String targetServer = "http://localhost:8080/matchboxv3/fhir";

  @BeforeClass
	public static void beforeClass() throws Exception {
		Path dir = Paths.get("database");
		if (Files.exists(dir)) {
			for (Path file : Files.list(dir).collect(Collectors.toList())) {
				if (Files.isRegularFile(file)) {
					Files.delete(file);
				}
			}	
		}
  }

  @BeforeAll void waitUntilStartup() throws InterruptedException {
    Thread.sleep(20000); // give the server some time to start up
    FhirContext contextR4 = FhirVersionEnum.R4.newContext();
    ValidationClient validationClient = new ValidationClient(contextR4, this.targetServer);
    validationClient.capabilities();
  }

  @Test
  public void validateRaw() {
    FhirContext contextR4 = FhirVersionEnum.R4.newContext();
    ValidationClient validationClient = new ValidationClient(contextR4, this.targetServer);
    String patient = "<Patient xmlns=\"http://hl7.org/fhir\">\n" + "            <id value=\"example\"/>\n"
        + "            <text>\n" + "               <status value=\"generated\"/>\n"
        + "               <div xmlns=\"http://www.w3.org/1999/xhtml\">42 </div>\n" + "            </text>\n"
        + "         </Patient>\n";
   
    IBaseOperationOutcome operationOutcome = validationClient.validate(patient,
        "http://hl7.org/fhir/StructureDefinition/Patient");
    assertEquals(0, IgValidateR4TestStandalone.getValidationFailures((OperationOutcome) operationOutcome));
    operationOutcome = validationClient.validate(patient,
        "http://hl7.org/fhir/StructureDefinition/Bundle");
    assertEquals(1, IgValidateR4TestStandalone.getValidationFailures((OperationOutcome) operationOutcome));
  }

  @Test
  //  https://gazelle.ihe.net/jira/browse/EHS-431
  public void validateEhs431() throws IOException {
    // 
    FhirContext contextR4 = FhirVersionEnum.R4.newContext();
    ValidationClient validationClient = new ValidationClient(contextR4, this.targetServer);

    validationClient.capabilities();

//    IBaseOperationOutcome operationOutcome = validationClient.validate(getContent("ehs-431.json"),
//        "http://fhir.ch/ig/ch-emed/StructureDefinition/ch-emed-document-medicationcard");
    IBaseOperationOutcome operationOutcome = validationClient.validate(getContent("ehs-431.json"),
        "http://hl7.org/fhir/StructureDefinition/Bundle");
    log.debug(contextR4.newJsonParser().encodeResourceToString(operationOutcome));
    assertEquals(1, IgValidateR4TestStandalone.getValidationFailures((OperationOutcome) operationOutcome));
  }

  @Test
  //  https://gazelle.ihe.net/jira/browse/EHS-419
  public void validateEhs419() throws IOException {
    // 
    FhirContext contextR4 = FhirVersionEnum.R4.newContext();
    ValidationClient validationClient = new ValidationClient(contextR4, this.targetServer);

    validationClient.capabilities();

    IBaseOperationOutcome operationOutcome = validationClient.validate(getContent("ehs-419.json"),
        "http://hl7.org/fhir/StructureDefinition/Patient");
    log.debug(contextR4.newJsonParser().encodeResourceToString(operationOutcome));
    assertEquals(0, IgValidateR4TestStandalone.getValidationFailures((OperationOutcome) operationOutcome));
  }
 
  private String getContent(String resourceName) throws IOException {
    Resource resource = new ClassPathResource(resourceName);
    File file = resource.getFile();
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
  }

}
