package org.operaton.rewrite.tests;

import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;

public class TryCatchAssertionsToAssertThatThrownBy extends Recipe {
  @Override
  public @NlsRewrite.DisplayName String getDisplayName() {
    return "Migrate tests using try-catch assertions to AssertJ assertThatThrownBy";
  }

  @Override
  public @NlsRewrite.Description String getDescription() {
    return "Migrate tests using try-catch assertions to AssertJ assertThatThrownBy.";
  }


}
