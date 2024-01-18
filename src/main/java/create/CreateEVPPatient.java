package create;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Address.AddressType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

public class CreateEVPPatient {

  private Patient patient = new Patient();

  public static void main(String[] args) {
    FhirContext fhirContext = FhirContext.forR4();
    IParser iParser = fhirContext.newXmlParser().setPrettyPrint(true);
    CreateEVPPatient evpPatient = new CreateEVPPatient();
    evpPatient.createEVPBasePatient();
    evpPatient.setKVID10("X123456789");
    evpPatient.setName("Peter", "Pan");
    evpPatient.setTelecom(ContactPointSystem.EMAIL, "test@test.org");
    evpPatient.setTelecom(ContactPointSystem.PHONE, "07132/865211");
    //evpPatient.setBirthdate("2001-11-01");
    evpPatient.setAddress("Musterstraße 3", "Musterstraße", "3", "Musterhausen", "77777");
    System.out.println(iParser.encodeResourceToString(evpPatient.patient));

  }

  private void setAddress(String line, String strasse, String nummer, String ort, String plz) {
    Address address = patient.addAddress();
    address.setType(AddressType.BOTH);
    //line
    StringType lineElement = address.addLineElement();
    lineElement.setValue(line);
    lineElement.addExtension()
        .setUrl("http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-streetName")
        .setValue(new StringType(strasse));
    lineElement.addExtension()
        .setUrl("http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-houseNumber")
        .setValue(new StringType(nummer));
    address.setCity(ort);
    address.setPostalCode(plz);
  }

  private Patient setBirthdate(String date) {
    DateType birthdate = new DateType(date);
    patient.setBirthDateElement(birthdate);
    return patient;
  }

  private Patient setTelecom(ContactPointSystem system, String value) {
    ContactPoint contactPoint = patient.addTelecom();
    contactPoint.setSystem(system);
    contactPoint.setValue(value);
    return patient;
  }

  private Patient setName(String vorname, String nachname) {
    HumanName humanName = patient.addName();
    humanName.setUse(NameUse.OFFICIAL);
    humanName.setFamily(nachname);
    humanName.addGiven(vorname);
    return patient;
  }

  private Patient setKVID10(String kvid10) {
    Identifier identifier = patient.addIdentifier();
    identifier.getType().addCoding().setSystem("http://fhir.de/CodeSystem/identifier-type-de-basis")
        .setCode("GKV");
    identifier.setSystem("http://fhir.de/sid/gkv/kvid-10").setValue(kvid10);
    return patient;
  }

  private Patient createEVPBasePatient() {

    patient.getMeta()
        .addProfile("https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_EVP_Versicherter|1.0");
    // set Patient active
    patient.setActive(true);
    patient.getBirthDateElement().addExtension()
        .setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason")
        .setValue(new CodeType("unknown"));
    return patient;
  }

}
