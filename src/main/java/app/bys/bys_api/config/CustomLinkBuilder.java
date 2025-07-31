package app.bys.bys_api.config;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

import java.util.Map;

public class CustomLinkBuilder extends StandardLinkBuilder {

    @Override
    protected String computeContextPath(
            final IExpressionContext context, final String base, final Map<String, Object> parameters) {

        if (context instanceof IWebContext) {
            return super.computeContextPath(context, base, parameters);
        }

        return "./src/main/resources/static"; //assuming css and images are here

    }

}
