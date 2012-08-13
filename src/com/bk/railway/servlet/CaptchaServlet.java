package com.bk.railway.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bk.railway.image.CaptchaResolver;

public class CaptchaServlet extends HttpServlet{
    private final static Logger LOG = Logger.getLogger(CaptchaServlet.class.getName());
    private static final long serialVersionUID = -7949807096913561091L;
    
    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        LOG.info("+" + getClass().getSimpleName() + " doPost");
        try {
            final CaptchaResolver captchaResolver = CaptchaResolver.getInstance();
            final BufferedImage rawImg = ImageIO.read(request.getInputStream());
            final String answer = captchaResolver.getCaptcha(rawImg);
   
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(answer);
            response.getWriter().flush();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        LOG.info("-" + getClass().getSimpleName() + " doPost");
    }
    
}
