package com.bazaarvoice.jolt;


import com.bazaarvoice.jolt.helper.Tester;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/templates")
public class ProductTemplates extends HttpServlet {
    public static final String TEMPLATE_YAML = "template.yaml";
    final Logger logger = Logger.getLogger(getClass().getName());
    public ProductTemplates(){
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException {
        logger.info("============== /templates  Start ");
        URI uri = null;
        try {
            uri = ProductTemplates.class.getResource("/").toURI();
            File yamlFile = new File(uri.getPath() + TEMPLATE_YAML);
            InputStream inputStream = Files.newInputStream(yamlFile.toPath());
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);
            String jsonString = JsonUtils.toJsonString(data);

            setResponseJSON(response, jsonString);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "URI formation is not proper");
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "YAML file path is not right");
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        logger.info("============== /templates  END ");
    }

    private static void setResponseJSON(HttpServletResponse response, String jsonString) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(jsonString);
        out.flush();
    }
}
