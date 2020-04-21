from PIL import Image, ImageFont, ImageDraw
import sys


# If there is at least one ttf font name in parameters
if len(sys.argv) <= 1:
    print("[ERROR] you need to give at least one ttf file font in parameters.")
    print("just write the ttf file names you want to be generated, seperated by a space character.")
    print("use a -d parameter to generate a debug version of your spritesheets.")
    exit()


#FONT_NAMES = ["arial", "comic","cour","impact","verdana"]

FONT_NAMES = sys.argv[1:]
DEBUG = False
if "-d" in FONT_NAMES:
    DEBUG = True
    FONT_NAMES.remove("-d")


TRANSPARENT_COLOR = (255,255,255,0)

FONT_SIZE = 120
IMG_W     = 1512
IMG_H     =  10
SPACEW    = 10
SPACEH    = 20

# Create alphabet
ALPHA = []
for n in range(32,127):
    ALPHA.append( chr(n) )
for n in range(161,384):
    ALPHA.append( chr(n) )


# init output hashmap
# key is the character
# value is a tuple containing the x/y top/left position and the width/height of the character in the spritesheet
OUT = {}

# process the character codes
for fontName in FONT_NAMES:
    print("============================"+(len(fontName)*"=")+"=====================")
    print("==================== FONT : "+fontName+" ====================")
    print("============================"+(len(fontName)*"=")+"=====================")
    # use a truetype font
    try:
        fnt = ImageFont.truetype(fontName + ".ttf", FONT_SIZE)
    except:
        try:
            fnt = ImageFont.truetype(fontName + ".TTF", FONT_SIZE)
        except:
            print("[ERROR] trying to open the " + fontName + " ttf file !")
            exit()
    # rename
    fontName = fontName[0].upper() + fontName[1:].lower()
    # Create font image
    im1 = Image.new('RGBA', (IMG_W, IMG_H), TRANSPARENT_COLOR)
    # create drawing instance
    draw = ImageDraw.Draw(im1)
    # Get height reference
    refX, refY = fnt.getsize("".join(ALPHA))

    # init spritesheet generation variables
    idx   = -1
    posX  =  SPACEW
    posY  =  0
    dhMax =  0

    # loop for each character
    for idx in range(len(ALPHA)):
        disp = ""

        # Get the char
        txt = ALPHA[idx]
        disp += "Character : "+txt

        # real size of the char
        dw, dh = fnt.getsize(txt)
        disp += " / "+str((posX,posY,dw,dh))

        # store max dh
        dhMax = max(dh,dhMax)
        disp += " / dhMax = "+str(dhMax)

        # Check the drawing is possible on the X AXIS
        if posX+dw+SPACEW >= im1.width :
            disp += "\n\n---------- New Line ----------"
            # reinit references
            posX  = SPACEW
            posY += dhMax+SPACEH
            dhMax = dh

        # check if the image buffer is still big enough (Y axis)
        if posY+dhMax+SPACEH >= im1.height:
            # create the new image with update dimension
            im2 = Image.new('RGBA', (IMG_W, posY+dhMax+SPACEH), TRANSPARENT_COLOR)
            # copy image
            im2.paste(im1, (0,0) )
            im1 = im2
            draw = ImageDraw.Draw(im1)

        print(disp)

        # draw the char
        draw.text((posX, posY), txt, font=fnt)
        # DEBUG draw a rectangle around the char
        if DEBUG == True:
            draw.rectangle( [posX,posY,posX+dw,posY+dh], outline=(0,0,0) )
        # fill the output
        key = txt
        value = (posX,posY,dw,dh)
        OUT[key] = value

        # increase position of X
        posX += dw+SPACEW

    # Store image
    outImagePath = "../resources/sprites/fonts/"+fontName + "SpriteFont.png"
    im1.save( outImagePath )
    im1.close()

    # ----------------------------------------------------------------------------
    # Store output data
    # ----------------------------------------------------------------------------
    # prepare output file path
    outDataPath = "../java/fr/rphstudio/ecs/component/render/font/"+fontName + "SpriteFont.java"
    # open file
    fp1 = open(outDataPath,"bw+")
    # add header
    fp1.write( "package fr.rphstudio.ecs.component.render.font; \n".encode() )
    fp1.write(("public class "+ fontName +"SpriteFont { \n").encode() )
    fp1.write( "    private static FontCharInfo[] letters = { \n".encode() )

    # add data
    for k in OUT.keys():
        ech = ""
        if k == "'" or k == "\\":
            ech = "\\"
        fp1.write(("        new FontCharInfo('"+ech+k+"', "+str(OUT[k][0])+", "+str(OUT[k][1])+", "+str(OUT[k][2])+", "+str(OUT[k][3])+"), \n").encode())

    # add footer
    fp1.write( "    }; \n".encode() )
    fp1.write( "    public static FontCharInfo[] getCharInfo() { \n".encode() )
    fp1.write(("        return "+ fontName +"SpriteFont.letters; \n").encode() )
    fp1.write( "    } \n".encode() )
    fp1.write( "} \n".encode() )

    # close file
    fp1.close()
