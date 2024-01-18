package validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import java.io.IOException;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.DefaultProfileValidationSupportNpmStrategy;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public class KBVForValidator {

  private final FhirContext ctx;
  private final FhirValidator validator;

  public KBVForValidator(FhirContext context) {
    this.ctx = context;
    NpmPackageValidationSupport npmPackageValidationSupport = new NpmPackageValidationSupport(ctx);
    try {
      npmPackageValidationSupport.loadPackageFromClasspath(
          "classpath:packages/de.basisprofil.r4-1.3.2.tgz");
      npmPackageValidationSupport.loadPackageFromClasspath(
          "classpath:packages/kbv.basis-1.3.0.tgz");
      npmPackageValidationSupport.loadPackageFromClasspath(
          "classpath:packages/kbv.ita.for-1.1.0.tgz");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ValidationSupportChain supportChain = new ValidationSupportChain(
        npmPackageValidationSupport,
        new DefaultProfileValidationSupport(ctx),
        new CommonCodeSystemsTerminologyService(ctx),
        new InMemoryTerminologyServerValidationSupport(ctx),
        new SnapshotGeneratingValidationSupport(ctx));
    CachingValidationSupport cachingValidationSupport = new CachingValidationSupport(supportChain);
    validator = ctx.newValidator();
    FhirInstanceValidator instanceValidator = new FhirInstanceValidator(cachingValidationSupport);
    validator.registerValidatorModule(instanceValidator);
  }

  public OperationOutcome validate(Resource resource) {
    ValidationResult validationResult = validator.validateWithResult(resource);
    return (OperationOutcome) validationResult.toOperationOutcome();
  }
}
