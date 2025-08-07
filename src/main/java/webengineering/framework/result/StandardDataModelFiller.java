package webengineering.framework.result;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class StandardDataModelFiller implements DataModelFiller {

    @Override
    public void fillDataModel(Map datamodel, HttpServletRequest request, ServletContext context) {
        datamodel.put("context_path", request.getContextPath());
    }
}
