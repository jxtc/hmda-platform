package hmda.api.http

import hmda.api.model.{ EditResult, EditResults, LarEditResult }
import hmda.model.fi.lar.LoanApplicationRegister
import hmda.model.util.FITestData._
import hmda.parser.fi.lar.LarCsvParser
import hmda.validation.context.ValidationContext
import hmda.validation.engine.lar.LarEngine
import org.scalatest.{ MustMatchers, WordSpec }

class ValidationErrorConverterSpec extends WordSpec with MustMatchers with ValidationErrorConverter with LarEngine {

  "Validation errors" must {
    "be converted to edit check summary" in {
      val badLars: Seq[LoanApplicationRegister] = fiCSVEditErrors.split("\n").tail.map(line => LarCsvParser(line).right.get)
      val ctx = ValidationContext(None, Some(2017))
      val errors = badLars.flatMap(lar => validationErrors(lar, ctx, validateLar).errors)
      val summaryEditResults = validationErrorsToEditResults(errors)
      val s020 = EditResult("S020", Seq(LarEditResult("8299422144"), LarEditResult("2185751599")))
      val s010 = EditResult("S010", Seq(LarEditResult("2185751599")))
      summaryEditResults.syntactical.edits.head mustBe s020
      summaryEditResults.syntactical.edits.tail.contains(s010) mustBe true
      summaryEditResults.validity.edits.size mustBe 3
      summaryEditResults.quality mustBe EditResults(Nil)
      summaryEditResults.`macro` mustBe EditResults(Nil)

    }
  }

}
