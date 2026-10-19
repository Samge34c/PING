import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Local, generic PING wordmark; JDK21 only. Run from repository root. */
public class GeneratePublicArtwork {
    private static void drawHeading(Graphics2D g,String text,int x,int y,float weight) {
        Shape outline=g.getFont().createGlyphVector(g.getFontRenderContext(),text).getOutline(x,y);
        g.fill(outline);g.setStroke(new BasicStroke(weight,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(outline);
    }
    public static void main(String[] args) throws Exception {
        Font font=Font.createFont(Font.TRUETYPE_FONT,Path.of("src/main/resources/static/fonts/Manrope-Variable.ttf").toFile());
        Path output=Path.of("src/main/resources/static/img");
        BufferedImage social=new BufferedImage(1200,630,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=social.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x101015));g.fillRect(0,0,1200,630);
        g.setColor(new Color(0x251333));g.fillRect(0,0,1200,270);
        g.setColor(new Color(0xf8e956));g.setFont(font.deriveFont(Font.BOLD,148));drawHeading(g,"PING.",72,204,3f);
        g.setColor(new Color(0xf5f3f8));g.setFont(font.deriveFont(Font.BOLD,62));drawHeading(g,"Conecta en tu evento.",72,390,1.3f);
        g.setFont(font.deriveFont(34f));g.drawString("Intereses compartidos. Encuentros en persona.",72,468);
        g.setColor(new Color(0xc7badb));g.setFont(font.deriveFont(25f));g.drawString("Proyecto académico",72,558);g.dispose();
        ImageIO.write(social,"PNG",output.resolve("ping-social.png").toFile());
        BufferedImage icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);g=icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x251333));g.fillRoundRect(0,0,32,32,8,8);g.setColor(new Color(0xf8e956));
        g.setFont(font.deriveFont(Font.BOLD,27));g.drawString("P",7,26);g.dispose();
        ImageIO.write(icon,"PNG",output.resolve("favicon-32.png").toFile());
    }
}
