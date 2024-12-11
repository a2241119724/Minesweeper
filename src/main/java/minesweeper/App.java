package minesweeper;

import processing.core.PApplet;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.*;

public class App extends PApplet {
    public static final int CELLSIZE = 32; //8;
    public static final int CELLHEIGHT = 32;
    public static final int CELLAVG = 32;
    public static final int TOPBAR = 64;
    public static int WIDTH = 864; //CELLSIZE*BOARD_WIDTH;
    public static int HEIGHT = 640; //BOARD_HEIGHT*CELLSIZE+TOPBAR;
    public static final int BOARD_WIDTH = WIDTH/CELLSIZE;
    public static final int BOARD_HEIGHT = 20;
    public static final int FPS = 30;
    public String configPath;
    public static Random random = new Random();
	public static int[][] mineCountColour = new int[][] {
            {0,0,0}, // 0 is not shown
            {0,0,255},
            {0,133,0},
            {255,0,0},
            {0,0,132},
            {132,0,0},
            {0,132,132},
            {132,0,132},
            {32,32,32}
    };

    private static final int mineImageCount = 10;
    private static final int wallImageCount = 1;
    private static final int flagImageCount = 1;
    private static final int tileImageCount = 3;
    private static final int x_len = 27;
    private static final int y_len = 18;
    private static final int size = 30;
    private static final int x_offset = 27;
    private static final int y_offset = 50;
    private static final int fontSize = 20;
    private static final int backgroundColor = GRAY;
    // 真实数据
    private static int[][] board = new int[x_len][y_len];
    // 展示 图片 内容
    private static int[][][] grids = new int[x_len][y_len][4];
    private static PImage[] flags_image = new PImage[flagImageCount];
    private static PImage[] mines_image = new PImage[mineImageCount];
    private static PImage[] tiles_image = new PImage[tileImageCount];
    private static PImage[] walls_image = new PImage[wallImageCount];
    private static int[][] direction = new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
    private static boolean isEnd = false;
    private static boolean isWin = false;
    private static int time = -1000;
    private static int _time = 0;
    private static int[][] mine_pos_explosion_time;
    private static int total_mine_count = 100;

    // Feel free to add any additional methods or attributes you want. Please put classes in different files.

    public App() {
        this.configPath = "config.json";
    }

    /**
     * Initialise the setting of the window size.
     */
	@Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    /**
     * Load all resources such as images. Initialise the elements such as the player and map elements.
     */
	@Override
    public void setup() {
        init();
        // 加载图片
        for (int i = 0; i < flagImageCount; i++) {
            flags_image[i] = loadImage(this.getClass().getResource("/minesweeper/flag"+i+".png").getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
        }
        for (int i = 0; i < mineImageCount; i++) {
            mines_image[i] = loadImage(this.getClass().getResource("/minesweeper/mine"+i+".png").getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
        }
        for (int i = 0; i < tileImageCount; i++) {
            tiles_image[i] = loadImage(this.getClass().getResource("/minesweeper/tile"+i+".png").getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
        }
        for (int i = 0; i < wallImageCount; i++) {
            walls_image[i] = loadImage(this.getClass().getResource("/minesweeper/wall"+i+".png").getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
        }
        //See PApplet javadoc:
		//loadJSONObject(configPath)
    }

    public void init(){
        // init
        frameRate(FPS);
        background(255);
        fill(backgroundColor);
        textSize(fontSize);
        // init show image
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                Arrays.fill(grids[i][j],-1);
                grids[i][j][1] = 1;
            }
        }
        // generate map
        for (int i = 0; i < x_len; i++) {
            Arrays.fill(board[i],0);
        }
        // -1:mine   0-8:around mine count
        mine_pos_explosion_time = new int[total_mine_count+1][3];
        int mine_count = total_mine_count;
        while(mine_count>0){
            int rand = random.nextInt(x_len*y_len);
            int x = rand / y_len;
            int y = rand % y_len;
            if(board[x][y]!=-1){
                board[x][y] = -1;
                mine_pos_explosion_time[total_mine_count-mine_count+1] = new int[]{x,y,0};
                mine_count--;
            }
        }
        // calc around mine's count
        int direction_len = direction.length;
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                if(board[i][j]!=-1){
                    for (int k = 0; k < direction_len; k++) {
                        int x = i-direction[k][0];
                        int y = j-direction[k][1];
                        if(isRange(x,y)&&board[x][y]==-1){
                            board[i][j]++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Receive key pressed signal from the keyboard.
     */
	@Override
    public void keyPressed(KeyEvent event){
        if(event.getKey()=='R'||event.getKey()=='r'){
            init();
            isEnd = false;
            isWin = false;
            time = -1000;
            _time = 0;
        }
    }

    /**
     * Receive key released signal from the keyboard.
     */
	@Override
    public void keyReleased(){

    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = (e.getX()-x_offset)/size;
        int y = (e.getY()-y_offset)/size;
        if(!isRange(x,y)||grids[x][y][GRID.WALL.index]>=0||isEnd||isWin){
            return;
        }
        if(e.getButton()==RIGHT){
            grids[x][y][GRID.FLAG.index] = grids[x][y][GRID.FLAG.index]==0?-1:0;
        }else if(e.getButton()==LEFT){
            if(isRange(x,y)&&grids[x][y][GRID.FLAG.index]<0){
                if(board[x][y]==-1){
                    // mine
                    Arrays.fill(grids[x][y],-1);
                    grids[x][y][GRID.MINE.index] = 0;
                    mine_pos_explosion_time[0][0] = x;
                    mine_pos_explosion_time[0][1] = y;
                    isEnd = true;
                }else{
                    Arrays.fill(grids[x][y],-1);
                    grids[x][y][GRID.WALL.index] = 0;
                    if(board[x][y]==0){
                        showAround(x,y);
                    }
                }
            }
        }
        isWin();
        if(isEnd){
            text("Game over!",WIDTH/2-50,30);
        }else if(isWin){
            text("You win!",WIDTH/2-50,30);
        }

    }

    public void explosion(){
        int len = mine_pos_explosion_time.length;
        for (int i = 0; i < len; i++) {
            int x = mine_pos_explosion_time[i][0];
            int y = mine_pos_explosion_time[i][1];
            if(grids[x][y][GRID.MINE.index]>=mineImageCount) continue;
            if(grids[x][y][GRID.MINE.index]==-1){
                mine_pos_explosion_time[i][2]++;
                int isSwitch = random.nextInt(3);
                if(isSwitch>=(3-mine_pos_explosion_time[i][2])){
                    grids[x][y][GRID.FLAG.index] = -1;
                    grids[x][y][GRID.MINE.index] = 0;
                    mine_pos_explosion_time[i][2] = 0;
                }
                return;
            }else{
                grids[x][y][GRID.TILE.index] = -1;
                grids[x][y][GRID.WALL.index] = 0;
                grids[x][y][GRID.MINE.index]++;
            }
        }
    }

    public void isWin(){
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                if(grids[i][j][GRID.TILE.index]>=0&&board[i][j]>=0){
                    isWin = false;
                    return;
                }
            }
        }
        isWin = true;
    }

    public void showAround(int centerX,int centerY){
        List<int[]> list = new ArrayList<>();
        int len = direction.length;
        for (int i = 0; i < len; i++) {
            int x = centerX-direction[i][0];
            int y = centerY-direction[i][1];
            if(isRange(x,y)&&board[x][y]>=0){
                if(grids[x][y][GRID.WALL.index]<0&&grids[x][y][GRID.FLAG.index]<0&&board[x][y]==0){
                    System.out.println(x+" "+y);
                    list.add(new int[]{x,y});
                }
                if(grids[x][y][GRID.FLAG.index]<0){
                    Arrays.fill(grids[x][y],-1);
                    grids[x][y][GRID.WALL.index] = 0;
                }
            }
        }
        len = list.size();
        for (int i = 0; i < len; i++) {
            showAround(list.get(i)[0],list.get(i)[1]);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = (e.getX()-x_offset)/size;
        int y = (e.getY()-y_offset)/size;
        if(isRange(x,y)&&grids[x][y][GRID.TILE.index]==1){
            for (int i = 0; i < x_len; i++) {
                for (int j = 0; j < y_len; j++) {
                    if(grids[i][j][GRID.TILE.index]>=0){
                        grids[i][j][GRID.TILE.index] = 1;
                    }
                }
            }
            grids[x][y][GRID.TILE.index] = 2;
        }
    }

    public void showTime(){
        _time = _time+FPS;
        if(_time/1000>time/1000){
            time = _time;
            background(255);
            textSize(25);
            text("Time:"+time/1000,WIDTH-150,30);
            textSize(fontSize);
        }
    }

    /**
     * Draw all elements in the game by current frame.
     */
	@Override
    public void draw() {
        if(isEnd){
            explosion();
        }
        if(!isEnd&&!isWin){
            showTime();
        }
        //draw game board
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                if(grids[i][j][GRID.WALL.index]>=0&&grids[i][j][GRID.WALL.index]<wallImageCount){
//                    image(walls_image[grids[i][j][GRID.WALL.index]],x_offset+i*size,y_offset+j*size);
                    image(tiles_image[0],x_offset+i*size,y_offset+j*size);
                    if(board[i][j]>0&&board[i][j]<10){
                        int[] color = mineCountColour[board[i][j]];
                        fill(color[0],color[1],color[2]);
                        text(board[i][j],x_offset+(i+0.5f)*size-fontSize/4,y_offset+(j+0.5f)*size+fontSize/3);
                        fill(backgroundColor);
                    }
                }
                if (grids[i][j][GRID.TILE.index]>=0&&grids[i][j][GRID.TILE.index]<tileImageCount) {
                    image(tiles_image[grids[i][j][GRID.TILE.index]],x_offset+i*size,y_offset+j*size);
                }
                if (grids[i][j][GRID.FLAG.index]>=0&&grids[i][j][GRID.FLAG.index]<flagImageCount) {
                    image(flags_image[grids[i][j][GRID.FLAG.index]],x_offset+i*size,y_offset+j*size);
                }
                if (grids[i][j][GRID.MINE.index]>=0&&grids[i][j][GRID.MINE.index]<mineImageCount) {
                    image(mines_image[grids[i][j][GRID.MINE.index]],x_offset+i*size,y_offset+j*size);
                }
            }
        }
    }

    public boolean isRange(int x, int y){
        return x>=0&&x<x_len&&y>=0&&y<y_len;
    }

    public static void main(String[] args) {
        if(args.length>0){
            try {
                System.out.println(args[0]);
                int mine_count = Integer.valueOf(args[0]);
                if(mine_count<=x_len*y_len){
                    total_mine_count = Integer.valueOf(args[0]);
                }
            }catch (Exception e){}
        }
        PApplet.main("minesweeper.App");
    }
}

enum GRID{
    WALL(0), TILE(1),FLAG(2),MINE(3);
    public final int index;
    GRID(int index){
        this.index = index;
    }
}
