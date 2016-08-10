package uk.gov.justice.services.example.cakeshop.command.api;

import org.junit.Test;

import static uk.gov.justice.services.test.utils.helper.ServiceComponents.verifyPassThroughCommandHandlerMethod;

public class RemoveRecipeCommandApiTest {

    @Test
    public void shouldHandleAddRecipeCommand() throws Exception {
        verifyPassThroughCommandHandlerMethod(RemoveRecipeCommandApi.class);
    }
}
