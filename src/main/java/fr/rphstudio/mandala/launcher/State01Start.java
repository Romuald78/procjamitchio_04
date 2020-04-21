/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.rphstudio.mandala.launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.rphstudio.ecs.component.render.RenderFont;
import fr.rphstudio.ecs.component.render.font.MandalaSpriteFont;
import fr.rphstudio.utils.rng.Prng;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

public class State01Start extends BasicGameState
{   
    //------------------------------------------------
    // PUBLIC CONSTANTS
    //------------------------------------------------
    public static final int ID = 100;


    //------------------------------------------------
    // PRIVATE PARAMETERS
    //------------------------------------------------
    private static int MIN_CIRCLES = 50;
    private static int MAX_CIRCLES = 75;
    private static int REF_RADIUS  = 500;
    private static int REF_SPEED   = 80;
    private static int MIN_SPEED   = 30;
    private static int REF_PULSE   = 10;
    private static int MAX_PULSE   = 100;            // percent of h1
    private static int NB_LEAF_TYPES = 7;
    private static int RANGE_MIN_CLR = 64;
    private static int MANDALA_TRANSPARENCY = 140;
    private static int CIRCLE_TRANSPARENCY = 70;
    private static int CIRCLE_FADE_TIME_MS = 10;
    private static int CIRCLE_FADE_OFFSET_MS = 150;



    private static Color BORDER_COLOR = new Color(255,255,255,255);
    private static Color BGND_COLOR = Color.black;

    //------------------------------------------------
    // PRIVATE PROPERTIES
    //------------------------------------------------
    private StateBasedGame gameObject;
    private GameContainer  container;
    private String         version;
    
    private Image          backGround;
    private RenderFont     txtMandala;
    private RenderFont     txtTitle;
    private RenderFont     txtSeed;
    private RenderFont     txtPrevNext;
    private Prng           prng;

    private int             numMandala;
    private int             currentSeed;
    private List<MandalaCircle> circles;
    private long            startRenderTime;


    //------------------------------------------------
    // PRIVATE Structures
    //------------------------------------------------
    private class MandalaCircle{
        private MandalaCircle(int s, int sClr)
        {
            // Get PRNGs
            Prng rng        = new Prng(s);
            Prng rngClr     = new Prng(sClr);
            this.radius     = (int)(rng.random()*REF_RADIUS);
            int minR   = (int)(rngClr.random()*RANGE_MIN_CLR)+RANGE_MIN_CLR;
            int minG   = (int)(rngClr.random()*RANGE_MIN_CLR)+RANGE_MIN_CLR;
            int minB   = (int)(rngClr.random()*RANGE_MIN_CLR)+RANGE_MIN_CLR;
            int rangeR = (int)(rngClr.random()*(255-minR));
            int rangeG = (int)(rngClr.random()*(255-minG));
            int rangeB = (int)(rngClr.random()*(255-minB));
            // Get random color now
            int rr = minR+(int)(rng.random()*rangeR);
            int gg = minG+(int)(rng.random()*rangeG);
            int bb = minB+(int)(rng.random()*rangeB);
            this.color  = new Color(rr,gg,bb,MANDALA_TRANSPARENCY);
            this.color2 = new Color(rr,gg,bb,CIRCLE_TRANSPARENCY);
            this.img        = getMandalaLeaf(this.radius,rng);
            this.globalRot  = (int)(rng.random()*REF_SPEED)+MIN_SPEED;
            this.singleRot  = (int)(rng.random()*REF_SPEED)+MIN_SPEED;
            this.clockWise = true;
            if(rng.random()>0.5){
                this.clockWise = false;
            }
            this.pulseFreq = (int)(rng.random()*REF_PULSE)+(REF_PULSE/2);
        }
        private float radius;
        private Image img;
        private int   globalRot;
        private int   singleRot;
        private Color color;
        private Color color2;
        private boolean clockWise;
        private int   pulseFreq;
    }


    //------------------------------------------------
    // PRIVATE METHODS
    //------------------------------------------------
    // Get current program version string from file
    private void getVersion()
    {
        // Get display version
        BufferedReader br = null;
        try
        {
            this.version = "";
            br = new BufferedReader(new FileReader("info/version.txt"));
            String line;
            line = br.readLine();
            while(line != null)
            {
                this.version = this.version + line + "\n";
                line = br.readLine();
            }
            if (br != null)
            {
                br.close();
            }
        }
        catch (IOException e)
        {
            throw new Error(e);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ex)
            {
                throw new Error(ex);
            }
        }
    }
    // Quit game
    private void quitGame()
    {
        this.container.exit();
    }


    private void adaptLuminance(){
        float max = 0;
        for(MandalaCircle m:this.circles){
            float y = (m.color.r*0.2126f) + (m.color.g*0.7152f) + (m.color.b*0.0722f);
            max = Math.max(max,y);
        }
        // Get the multiplier ratio from the max luminance
        float ratio = 1.0f/max;
        // Multiply each component for each circle color
        for(MandalaCircle m:this.circles){
            m.color.r  *= ratio;
            m.color.g  *= ratio;
            m.color.b  *= ratio;
            m.color2.r *= ratio;
            m.color2.g *= ratio;
            m.color2.b *= ratio;
        }
    }
    private Color getMinLuminanceColor()
    {
        Color res = Color.white;
        float min = 1000000000;
        for(MandalaCircle m:this.circles){
            float y = (m.color.r*0.2126f) + (m.color.g*0.7152f) + (m.color.b*0.0722f);
            if(y < min)
            {
                min = y;
                res = new Color(m.color.r, m.color.g,m.color.b,1.0f);
            }
        }
        return res;
    }

    private Image getStarLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        Polygon sh = new Polygon();
        sh.addPoint(0,h1/2);
        sh.addPoint(3*w1/8,5*h1/8);
        sh.addPoint(w1/2,h1);
        sh.addPoint(5*w1/8,5*h1/8);
        sh.addPoint(w1,h1/2);
        sh.addPoint(5*w1/8,3*h1/8);
        sh.addPoint(w1/2,0);
        sh.addPoint(3*w1/8,3*h1/8);
        g1.fill(sh);
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.draw(sh);
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getLosangeLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        Polygon sh = new Polygon();
        sh.addPoint(0,h1/2);
        sh.addPoint(w1/2,h1);
        sh.addPoint(w1,h1/2);
        sh.addPoint(w1/2,0);
        g1.fill(sh);
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.draw(sh);
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getClubLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        g1.fillRect(w1/4,h1/4,w1/2,h1/2);
        g1.fillOval(w1/4,0,w1/2,h1/2);
        g1.fillOval(0,h1/4,w1/2,h1/2);
        g1.fillOval(w1/2,h1/4,w1/2,h1/2);
        g1.fillOval(w1/4,h1/2,w1/2,h1/2);
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.drawRect(w1/4,h1/4,w1/2,h1/2);
        g1.drawOval(w1/4,0,w1/2,h1/2);
        g1.drawOval(0,h1/4,w1/2,h1/2);
        g1.drawOval(w1/2,h1/4,w1/2,h1/2);
        g1.drawOval(w1/4,h1/2,w1/2,h1/2);
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getSquareLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        g1.fillRoundRect(0, 0, w1, h1, (int)(w1/2.5f));
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.drawRoundRect(0, 0, w1, h1, (int)(w1/2.5f));
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getOvalLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        g1.fillOval(0, 0, w1, h1);
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.drawOval(0, 0, w1, h1);
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getRibbedLeaf2(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        g1.fillArc(0, 0, 2 * w1, h1, 140, 240);
        g1.fillArc(0 - w1, 0, 2 * w1, h1, 300, 400);
        // draw borders
        g1.setColor(BORDER_COLOR);
        g1.drawArc(0, 0, 2 * w1, h1, 140, 240);
        g1.drawArc(0 - w1, 0, 2 * w1, h1, 300, 400);
        // flush
        g1.flush();
        // return image
        return im1;
    }
    private Image getRibbedLeaf(int w1, int h1) throws SlickException
    {
        Image im1 = new Image(w1,h1);
        Graphics g1 = im1.getGraphics();
        // Draw leaf
        g1.setColor(Color.white);
        g1.fillArc(0, 0, 2 * w1, 1.6f*h1, 180, 240);
        g1.fillArc(0 - w1, 0, 2 * w1, 1.6f*h1, 300, 360);
        g1.fillArc(0, 0.6f*h1, w1, 0.4f*h1,0,180);
        // Draw borders
        g1.setColor(BORDER_COLOR);
        g1.drawArc(0, 0, 2 * w1, 1.6f*h1, 180, 240);
        g1.drawArc(0 - w1, 0, 2 * w1, 1.6f*h1, 300, 360);
        g1.drawArc(0, 0.6f*h1, w1, 0.4f*h1,0,180);
        // flush
        g1.flush();
        // return image
        return im1;
    }

    // Get a mandala leaf image
    private Image getMandalaLeaf(float rad, Prng rng)
    {
        Image img = null;
        // Set dimensions of one leaf
        int w1 = (int)(rng.random()*(rad))+50;          // Max size of w1 is rad/4
        w1 = Math.min(w1,(int)(rad/3));
        w1 = Math.max(w1,50);
        int h1 = (int)(rng.random()*(rad-w1)+w1);       // Max size of h1 is radius/2 and must be at least equals to w1
        h1 = Math.max(h1,w1);
        h1 = Math.min(h1,(int)(rad/2));
        // Create image for one leaf
        try {
            int N = (int)(rng.random()*NB_LEAF_TYPES);
            switch(N){
                case 0:
                    img = getRibbedLeaf(w1,h1);
                    break;
                case 1:
                    img = getRibbedLeaf2(w1,h1);
                    break;
                case 2:
                    img = getOvalLeaf(w1,h1);
                    break;
                case 3:
                    // Here h1 = w1
                    h1 = w1;
                    img = getSquareLeaf(w1,h1);
                    break;
                case 4:
                    img = getClubLeaf(w1,h1);
                    break;
                case 5:
                    img = getLosangeLeaf(w1,h1);
                    break;
                case 6:
                    img = getStarLeaf(w1,h1);
                    break;
                default:
                    break;
            }
        }
        catch(SlickException se){
            throw new Error("Impossible to create image or get graphics (mandala leaf");
        }
        return img;
    }
    // Draw a new mandala circle
    private void drawMandalaCircle(float xc, float yc, Graphics g, MandalaCircle circle) throws SlickException
    {
        // Compute current angle (global)
        int moveAng = (int)(10*System.currentTimeMillis()/circle.globalRot);
        moveAng    %= 3600;
        if(!circle.clockWise){
            moveAng = 3600-moveAng;
        }
        float moveAngF = (float)moveAng/10;

        // Compute current angle (single leaf)
        int selfMoveAng = (int)(10*System.currentTimeMillis()/circle.singleRot);
        selfMoveAng    %= 3600;
        if(!circle.clockWise){
            selfMoveAng = 3600-selfMoveAng;
        }
        float selfMoveAngF = (float)selfMoveAng/10;

        // compute number of leaves according to the current radius and leaf width
        int nbLeaves = (int) (((circle.radius * 2 * Math.PI) / circle.img.getWidth()) + 1);
        nbLeaves = Math.max(nbLeaves,4); // At least 4 leaves per circle
        float stepAng = 360f / nbLeaves;


        int pulse = (int)(System.currentTimeMillis()/circle.pulseFreq)&0x7FFFFFFF;
        pulse %= 2*MAX_PULSE;
        if(pulse > MAX_PULSE){
            pulse = 2*MAX_PULSE - pulse;
        }
        pulse -= MAX_PULSE/2;
        pulse *= circle.img.getHeight();
        float pulseF = pulse/1000.0f;
        float fullRadius = circle.radius + pulseF;



        // Draw the mandala circle
        float ang = moveAngF;
        for (int lf=0; lf<nbLeaves; lf++) {
            circle.img.setRotation(ang + 90 + selfMoveAngF);
            g.drawImage( circle.img,
                         (float)(xc - (circle.img.getWidth()  / 2) + fullRadius * Math.cos(ang * Math.PI / 180)),
                         (float)(yc - (circle.img.getHeight() / 2) + fullRadius * Math.sin(ang * Math.PI / 180)),
                         circle.color
                       );
            ang += stepAng;
        }

        // Draw circle in the middle to avoid black holes
        g.setColor(circle.color2);
        g.fillOval(xc-circle.radius,yc-circle.radius,circle.radius*2, circle.radius*2);
    }

    // Change mandala
    private void nextMandala()
    {
        this.numMandala++;
        this.numMandala += 1000000000;
        this.numMandala %= 1000000000;
        this.init();
    }
    private void prevMandala()
    {
        this.numMandala--;
        this.numMandala += 1000000000;
        this.numMandala %= 1000000000;
        this.init();
    }


    
    //------------------------------------------------
    // CONSTRUCTOR
    //------------------------------------------------
    public State01Start()
    {
    }
    
    
    //------------------------------------------------
    // INIT METHOD
    //------------------------------------------------
    public void init(GameContainer container, StateBasedGame sbGame) throws SlickException
    {
        // Init fields
        this.container  = container;
        this.gameObject = sbGame;
        
        // Get version string
        this.getVersion();

        // Load background image
        this.backGround  = new Image("sprites/bgnd.png");

        // Load txtMandala component
        Image img = new Image("./sprites/fonts/MandalaSpriteFont.png");
        // Create font objects
        this.txtMandala  = new RenderFont("---test---", img, MandalaSpriteFont.getCharInfo(), 0, 0.75f, Color.white);
        this.txtTitle    = new RenderFont("---test---", img, MandalaSpriteFont.getCharInfo(), 0, 1.00f, Color.white);
        this.txtSeed     = new RenderFont("---test---", img, MandalaSpriteFont.getCharInfo(), 0, 0.50f, Color.white);
        this.txtPrevNext = new RenderFont("---test---", img, MandalaSpriteFont.getCharInfo(), 0, 0.35f, Color.white);
        // Set font positions
        this.txtMandala.setPosition(50,930);
        this.txtTitle.setPosition(400,70);
        this.txtSeed.setPosition(50,1000);
        this.txtPrevNext.setPosition(50,1050);
        // set font alignments
        this.txtMandala.setMiddleAlign(false);
        this.txtSeed.setMiddleAlign(false);
        this.txtPrevNext.setMiddleAlign(false);

        // Init process for the first time
        this.numMandala = 0;
        this.prng = new Prng();
        this.init();
    }

    public void init()
    {
        int seed = (((this.numMandala*123456789)+(this.numMandala*987654321)+(this.numMandala*755664488)+467913528)+123456789)&0x7FFFFFFF;
        this.prng.setSeed(seed);
        this.currentSeed = seed;

        // Set txtMandala message
        this.txtMandala.setMessage("Mandala #"+(this.numMandala+1));
        this.txtTitle.setMessage("Mandala Generator");
        this.txtSeed.setMessage("Seed = 0x"+Integer.toHexString(this.currentSeed).toUpperCase());
        this.txtPrevNext.setMessage("Use arrows to change");

        // Get number of circles
        int nbCircles = (int)(this.prng.random()*(MAX_CIRCLES-MIN_CIRCLES) + MIN_CIRCLES);
        // Create list of circles
        this.circles = new ArrayList<>();
        // Create seed for all circle colors
        int sClr = ((int) (this.prng.random() * 999999999)) & 0x7FFFFFFF;
        for(int i=0;i<nbCircles;i++) {
            // for each circle create seed
            int s = ((int) (this.prng.random() * 1000000000)) & 0x7FFFFFFF;
            this.circles.add(new MandalaCircle(s,sClr));
        }
        // sort mandala circles by radius + image height
        Collections.sort(this.circles, MandalaComparator);

        // Adapt luminance
        this.adaptLuminance();

        // store time for start render
        this.startRenderTime = System.currentTimeMillis();
    }


        
    //------------------------------------------------
    // RENDER METHOD
    //------------------------------------------------
    public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException
    {
        // Fit Screen
        MainLauncher.fitScreen(container, g);

        // Render Start screen background
        g.setColor(BGND_COLOR);
        g.drawImage(this.backGround, 0, 0);
//        g.fillRect(0,0,MainLauncher.WIDTH, MainLauncher.HEIGHT);

        // Main position and size of the mandala layer
        float xc   = MainLauncher.WIDTH/2f;
        float yc   = MainLauncher.HEIGHT/2f;



        // Draw each mandala circle (using the startRenderTime to display each circle one by one with fading)
        long elapsedTime = System.currentTimeMillis() - this.startRenderTime;
        elapsedTime -= CIRCLE_FADE_OFFSET_MS;
        elapsedTime = Math.max( elapsedTime, 0 );
        elapsedTime = Math.min( elapsedTime, CIRCLE_FADE_TIME_MS*this.circles.size() );
        int nbCirclesToDisplay = (int)(elapsedTime/CIRCLE_FADE_TIME_MS);
        for(int i=0; i<nbCirclesToDisplay; i++) {
            drawMandalaCircle(xc, yc, g, this.circles.get(i));
        }




        // Render messages
        Color minClr = this.getMinLuminanceColor();
        this.txtMandala.setColor(minClr);
        this.txtTitle.setColor(minClr);
        this.txtSeed.setColor(minClr);
        this.txtPrevNext.setColor(minClr);
        this.txtMandala.render(container, game, g );
        this.txtTitle.render(container, game, g );
        this.txtSeed.render(container, game, g );
        this.txtPrevNext.render(container, game, g );

        // Render version number
        g.setColor(minClr);
        g.drawString(this.version, 1920-500, 1080-30);
    }

    
    //------------------------------------------------
    // UPDATE METHOD
    //------------------------------------------------
    public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException
    {   

    }
    
    
    //------------------------------------------------
    // KEYBOARD METHODS
    //------------------------------------------------
    @Override
    public void keyPressed(int key, char c)
    {
        switch(key)
        {
            // Quit game by pressing escape
            case Input.KEY_ESCAPE:
                this.quitGame();
                break;
            case Input.KEY_LEFT:
                this.prevMandala();
                break;
            case Input.KEY_RIGHT:
                this.nextMandala();
                break;
            case Input.KEY_F11:
                try {
                    this.container.setFullscreen(!this.container.isFullscreen());
                }catch(SlickException se){}
                break;
            // go to game
            // all other keys have no effect
            default :     
                break;        
        }
    }
    
    
    //------------------------------------------------
    // STATE ID METHOD
    //------------------------------------------------
    @Override
    public int getID()
    {
          return this.ID;
    }



    public static Comparator<MandalaCircle> MandalaComparator = new Comparator<MandalaCircle>() {
        public int compare(MandalaCircle c1, MandalaCircle c2) {
            if( (c1.radius+(c1.img.getHeight()/2)) >= (c2.radius+(c2.img.getHeight()/2)) ){
                return -1;
            }
            else
            {
                return 1;
            }
        }
    };

            //------------------------------------------------
    // END OF STATE
    //------------------------------------------------
}