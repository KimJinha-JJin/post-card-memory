import com.postcardmemory.utils.StampPerforationKt;
import com.postcardmemory.utils.StampPerforationSink;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

/** Desktop geometry evidence, NOT an Android/Compose golden or device screenshot. */
public class StampPerforationPreview {
    static final double DENSITY = 2.8125; // Comparison condition, not a measured device setting.
    static BufferedImage photo;
    static Path2D c2(double side) {
        Path2D path = new Path2D.Double();
        StampPerforationKt.traceStampPerforation(new StampPerforationSink() {
            public void moveTo(double x, double y) { path.moveTo(x * side, y * side); }
            public void lineTo(double x, double y) { path.lineTo(x * side, y * side); }
            public void arcTo(double x, double y, double r, double start, double sweep) {
                path.append(new Arc2D.Double((x-r)*side, (y-r)*side, 2*r*side, 2*r*side,
                    -start, -sweep, Arc2D.OPEN), true);
            }
            public void close() { path.closePath(); }
        });
        return path;
    }
    // Day 134 createPinkingPath arithmetic, retained only as the before-reference.
    static Path2D before(float side, float inset) {
        float length = Math.max(side - 2*inset, 1f);
        float depth = Math.max(length*.032f, 2f), corner = depth*1.5f;
        float straight = Math.max(length-2*corner, 1f);
        int teeth = Math.max(8, (int)(straight/(depth*2.3f)));
        float step = straight/teeth;
        Path2D path = new Path2D.Double();
        path.moveTo(inset+corner, inset);
        for (int edge=0; edge<4; edge++) {
            for (int i=0; i<teeth; i++) {
                float start = inset+corner+i*step;
                point(path, edge, side, start+step/2f, inset+depth);
                point(path, edge, side, start+step, inset);
            }
            point(path, edge, side, side-inset, inset+corner);
        }
        path.closePath();
        return path;
    }
    static void point(Path2D path, int edge, double side, double x, double y) {
        switch (edge) {
            case 0 -> path.lineTo(x,y);
            case 1 -> path.lineTo(side-y,x);
            case 2 -> path.lineTo(side-x,side-y);
            default -> path.lineTo(y,side-x);
        }
    }
    static Graphics2D graphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }
    static BufferedImage render(double dp, boolean changed) {
        float side=(float)(dp*DENSITY);
        BufferedImage image=new BufferedImage((int)Math.ceil(side), (int)Math.ceil(side), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(image);
        Shape outer=changed?c2(side):before(side,0);
        g.setClip(outer);
        g.setColor(new Color(0xc5c1ad)); g.fill(outer);
        double scale=Math.max(side/photo.getWidth(),side/photo.getHeight());
        AffineTransform tx=new AffineTransform();
        tx.translate((side-photo.getWidth()*scale)/2, (side-photo.getHeight()*scale)/2);
        tx.scale(scale,scale); g.drawImage(photo,tx,null);
        if (!changed) g.setClip(null);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float)(3*DENSITY*(changed?2:1)), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(changed?outer:before(side,(float)(1.5*DENSITY)));
        g.dispose(); return image;
    }
    static void label(Graphics2D g, String text, int x,int y) {
        g.setColor(new Color(0x333333)); g.setFont(new Font("SansSerif",Font.PLAIN,17)); g.drawString(text,x,y);
    }
    static BufferedImage shadowSample(double dp, int alpha, double dx, double dy) {
        double side=dp*DENSITY, scale=side/1000.0, inset=9*scale;
        int margin=25;
        BufferedImage image=new BufferedImage((int)Math.ceil(side)+2*margin,
            (int)Math.ceil(side)+2*margin,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(image); g.translate(margin,margin);
        Graphics2D shadow=(Graphics2D)g.create();
        shadow.translate(dx*scale+inset,dy*scale+inset);
        shadow.setColor(new Color(18,12,28,alpha)); shadow.fill(c2(side-2*inset)); shadow.dispose();
        Shape outer=c2(side); g.setClip(outer); g.setColor(new Color(0xc5c1ad)); g.fill(outer);
        double imageScale=Math.max(side/photo.getWidth(),side/photo.getHeight());
        AffineTransform tx=new AffineTransform();
        tx.translate((side-photo.getWidth()*imageScale)/2,(side-photo.getHeight()*imageScale)/2);
        tx.scale(imageScale,imageScale); g.drawImage(photo,tx,null);
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke((float)(36*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(outer); g.dispose(); return image;
    }
    static void shadowComparison(Path out) throws Exception {
        int[] alpha={85,30,18}; double[] dx={22,16,11},dy={26,18,13};
        String[] titles={"CURRENT","WEAKER","MICRO RELIEF"};
        BufferedImage sheet=new BufferedImage(1470,1130,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=graphics(sheet); g.setColor(new Color(0xfffcf5));g.fillRect(0,0,1470,1130);
        label(g,"Detail/export shadow comparison | identical C2 | existing inset 9 | blur 0 throughout",20,28);
        label(g,"Logical photo side 1000. Lower alpha + shorter offset; no new blur, highlight or emboss filter.",20,53);
        for(int column=0;column<3;column++) {
            int x=column*490;
            label(g,titles[column],x+20,90);
            label(g,"alpha "+alpha[column]+"/255, offset "+(int)dx[column]+"/"+(int)dy[column],x+20,115);
            label(g,"133.33dp detail-size sample",x+20,143);
            g.drawImage(shadowSample(133.333333,alpha[column],dx[column],dy[column]),x+5,150,null);
            label(g,"60dp",x+20,620);
            BufferedImage small=shadowSample(60,alpha[column],dx[column],dy[column]);
            g.drawImage(small,x+5,630,null);
            label(g,"38dp",x+20,910);
            g.drawImage(shadowSample(38,alpha[column],dx[column],dy[column]),x+5,920,null);
            label(g,"60dp bottom-right, 6x",x+220,620);
            int end=small.getWidth()-25;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(small,x+220,655,x+460,895,end-30,end-30,end+10,end+10,null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        g.dispose();ImageIO.write(sheet,"png",out.resolve("c2-shadow-comparison.png").toFile());
    }
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]), out=Path.of(args[1]); Files.createDirectories(out);
        String html=Files.readString(root.resolve("docs/mockups/day135-stamp-edge-study/index.html"));
        var match=Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)").matcher(html);
        if (!match.find()) throw new IllegalStateException("No existing mockup photo");
        photo=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(match.group(1))));
        double[] sizes={93.333333,106.666667,146.666667,60,38};
        int[] xs={25,320,650,1100,1300};
        BufferedImage sheet=new BufferedImage(1450,1020,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=graphics(sheet); g.setColor(new Color(0xd8d0bf)); g.fillRect(0,0,1450,1020);
        label(g,"C2 production geometry | desktop Java2D surrogate | density 2.8125 | no shadow",25,28);
        label(g,"BEFORE: original gallery outline / 3dp white rim",25,60);
        label(g,"AFTER: approved C2 / same crop / 3dp inner white rim",25,535);
        long changedTotal=0, interiorTotal=0;
        for(int i=0;i<sizes.length;i++) {
            BufferedImage a=render(sizes[i],false), b=render(sizes[i],true);
            label(g,String.format(java.util.Locale.ROOT,"%.2fdp",sizes[i]),xs[i],90);
            g.drawImage(a,xs[i],105,null); g.drawImage(b,xs[i],560,null);
            int margin=(int)Math.ceil(12*DENSITY); long changed=0, interior=0;
            for(int y=0;y<a.getHeight();y++) for(int x=0;x<a.getWidth();x++) {
                if(a.getRGB(x,y)!=b.getRGB(x,y)) {
                    changed++;
                    if(x>=margin&&y>=margin&&x<a.getWidth()-margin&&y<a.getHeight()-margin) interior++;
                }
            }
            changedTotal+=changed; interiorTotal+=interior;
            System.out.printf(java.util.Locale.ROOT,"%.6fdp: changed=%d interiorBeyond12dp=%d%n",sizes[i],changed,interior);
        }
        g.dispose(); ImageIO.write(sheet,"png",out.resolve("c2-before-after.png").toFile());
        BufferedImage corners=new BufferedImage(1240,850,BufferedImage.TYPE_INT_RGB);
        g=graphics(corners); g.setColor(new Color(0xd8d0bf)); g.fillRect(0,0,1240,850);
        label(g,"60dp, 6x nearest-neighbour enlargement | BEFORE top / C2 bottom",20,28);
        BufferedImage a=render(60,false),b=render(60,true);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        String[] names={"TOP LEFT","TOP RIGHT","BOTTOM RIGHT","BOTTOM LEFT"};
        for(int row=0;row<2;row++) for(int corner=0;corner<4;corner++) {
            BufferedImage image=row==0?a:b; int crop=45;
            int sx=(corner==1||corner==2)?image.getWidth()-crop:0;
            int sy=(corner>=2)?image.getHeight()-crop:0;
            int x=20+corner*305,y=75+row*375;
            label(g,names[corner],x,y-12);
            g.drawImage(image,x,y,x+270,y+270,sx,sy,sx+crop,sy+crop,null);
        }
        g.dispose(); ImageIO.write(corners,"png",out.resolve("c2-corners-6x.png").toFile());
        if(interiorTotal!=0||changedTotal==0) throw new AssertionError("Unexpected comparison region");
        shadowComparison(out);
    }
}
