package com.bazaarvoice.jolt;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

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
