package extract;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.parser.IParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;

public class FHIRHandler {

  private final FhirContext ctx;
  private final IParser parser;

  public FHIRHandler() {
    this.ctx = FhirContext.forR4();
    this.parser = ctx.newXmlParser();
  }


  public void handleFhirString(String bundleString) {
    Bundle bundle = parser.parseResource(Bundle.class, bundleString);
    List<Bundle> erezptListe = bundle.getEntry().stream()
        .filter(e -> e.getResource().getResourceType() == ResourceType.Bundle)
        .map(be -> (Bundle) be.getResource()).toList();
    System.out.println("Gefundene eRezepte: " + erezptListe.size());
    erezptListe.forEach(this::getBundlesFromBinaries);

  }

  private void getBundlesFromBinaries(Bundle bundle) {
    System.out.println("=================");
    System.out.println("Bundle: " + bundle.getId());
    List<Binary> binaries = bundle.getEntry().stream()
        .filter(e -> e.getResource().getResourceType() == ResourceType.Binary)
        .map(ec -> (Binary) ec.getResource()).toList();
    List<Bundle> bundles = binaryToBundle(binaries);
    Optional<Bundle> first = bundles.stream().filter(
        b -> b.getMeta().getProfile().get(0).getValueAsString()
            .contains("https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle")).findFirst();
    handleERPBundle(first.get());

  }

  private void handleERPBundle(Bundle bundle) {
    extractPatientData(bundle);
    extractPZNMedication(bundle);
  }

  private void extractPZNMedication(Bundle bundle) {
    IFhirPath fhirPath = ctx.newFhirPath();
    Optional<Medication> medicationOptional = fhirPath.evaluateFirst(bundle,
        "entry.where(resource.meta.profile.contains('https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Medication_PZN')).resource",
        Medication.class);
    // PZN Verordnung muss nicht vorhanden sein
    if(medicationOptional.isPresent()) {
      Medication medication = medicationOptional.get();
      System.out.println("PZN Medikament: " + medication.getCode().getCodingFirstRep().getCode());
    }
  }

  private void extractPatientData(Bundle bundle) {
    Optional<Patient> patientOptional = bundle.getEntry().stream()
        .filter(e -> e.getResource().getResourceType() == ResourceType.Patient)
        .map(ec -> (Patient) ec.getResource()).findFirst();
    Patient patient = patientOptional.get();
    System.out.println("Nachname: " + patient.getNameFirstRep().getFamily());
    System.out.println("Vornamen: " + patient.getNameFirstRep().getGivenAsSingleString());
    System.out.println("Geburtsdatum " + patient.getBirthDate().toString());

    // KVID-10 extrahieren
    Optional<Identifier> kvid10Identifier = patient.getIdentifier().stream()
        .filter(i -> i.getSystem().equals("http://fhir.de/sid/gkv/kvid-10")).findFirst();
    String kvid10 = kvid10Identifier.get().getValue();
    System.out.println("Versichertennummer: " + kvid10);

    //alternatives Vorgehen mit FHIRPATH: https://hl7.org/fhir/fhirpath.html
    // FHIRPath: https://fhirpath-lab.com/FhirPath
    IFhirPath fhirPath = ctx.newFhirPath();
    Optional<StringType> identifier = fhirPath.evaluateFirst(patient,
        "identifier.where(system = 'http://fhir.de/sid/gkv/kvid-10').value", StringType.class);
    System.out.println("Kvid10 durch Fhirpath selektiert: " + identifier.get().getValue());

  }

  private List<Bundle> binaryToBundle(List<Binary> binaries) {
    List<Bundle> bundles = new ArrayList<>();
    binaries.forEach(b -> {
      String bundleString = new String(b.getContent(), StandardCharsets.UTF_8);
      Bundle bundle = parser.parseResource(Bundle.class, bundleString);
      bundles.add(bundle);
    });
    return bundles;
  }
}
