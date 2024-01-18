package example;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

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
    getBundlesFromBinaries(erezptListe.get(0));

  }

  private void getBundlesFromBinaries(Bundle bundle) {
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
