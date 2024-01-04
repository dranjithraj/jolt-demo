package com.bazaarvoice.jolt;

import com.bazaarvoice.jolt.helper.Item;
import com.bazaarvoice.jolt.helper.ItemsList;
import com.bazaarvoice.jolt.helper.MainClass;
import freemarker.template.Configuration;
import freemarker.template.Template;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@WebServlet("/generate")
public class GenerateSpec extends HttpServlet {

    public static final String UNDERSCORE = "_";
    public static final String INPUT = "input";
    public static final String USER_CREATE_CONFIG_FTL = "user_create_config.ftl";
    public static final String MODEL_PROPERTIES = "model_properties";
    public static final String PAYLOAD = "payload";
    final Logger logger = Logger.getLogger(getClass().getName());

    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        logger.info("===========/generate POST Request ===========");
        logger.info("input-> " + req.getParameter("input"));

        String inputPayloadString;
        try {
            inputPayloadString = req.getParameter(INPUT);
        } catch (Exception e) {
            response.getWriter().println("Could not url-decode the inputs.\n");
            return;
        }

        Object inputPayloadJSON;
        Writer writer = new StringWriter();
        Configuration configuration = new Configuration();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        List<Item> items = new ArrayList<>();
        ItemsList itemsList = new ItemsList(items);
        try {
            inputPayloadJSON = JsonUtils.jsonToObject(inputPayloadString);
            if (inputPayloadJSON instanceof LinkedHashMap) {
                linkedHashMap = (LinkedHashMap) inputPayloadJSON;
            } else if (inputPayloadJSON instanceof ArrayList){
                linkedHashMap = (LinkedHashMap) ((ArrayList)inputPayloadJSON).get(0);
            }
            LinkedHashMap payloadJson = (LinkedHashMap)linkedHashMap.get(PAYLOAD);
            Set keys = ((LinkedHashMap) payloadJson.get(MODEL_PROPERTIES)).keySet();

            keys.forEach(key -> {
                if (!(((LinkedHashMap) payloadJson.get(MODEL_PROPERTIES)).get(key.toString()) instanceof ArrayList))
                    items.add(new Item(key.toString(), 0));
            });

            configuration.setDirectoryForTemplateLoading(new File(MainClass.class.getResource("/").toURI()));
            Template template = configuration.getTemplate(USER_CREATE_CONFIG_FTL);

            template.process(itemsList, writer);

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Could not parse the 'input' JSON.\n");
            return;
        }

        String result = writer.toString();
        logger.info("Java GenerateSpec result=> "+ result);

        // Add PR details in the response
        response.getWriter().println(result);
    }

}
