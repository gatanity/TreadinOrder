package treadinorder.gamepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import treadinorder.MainPanel;
import treadinorder.TOUtils;
import treadinorder.TreadinOrder.Tiles;

public class GamePlayPanel extends JPanel {
	// クラス変数
	// プレイヤー画像の相対パス
	public static final String PLAYERIMAGE_PATH = "../assets/player.png";
	// ゴール画像の相対パス
	public static final String GOALIMAGE_PATH = "../assets/goal.png";
	
	// インスタンス変数
	private MainPanel mPanel;	// メインパネル
	
	private int[][] map;			// 迷路マップ
	private int mapSize;			// マップの大きさ
	
	private List<Integer> nowTread = new ArrayList<Integer>();			// 今踏んでいるタイル
	private int lastTrod;															// 最後に踏んだタイル
	private int[][] trodTiles;													// 既に踏んだタイル
	
	private int topBottomSpace;	// 上下余白
	
	private int oneset;			// 指定される順番のワンセット
	private int tileDrawsize;	// タイルの描画する大きさ
	
	private Image[] scaledTileImages;	// スケールされたタイル画像配列
	private Image[][] mapTiles;			// 迷路マップの画像版
	
	private int playerWidth, playerHeight;	// プレイヤーの大きさ
	private int playerX, playerY;				// プレイヤーの座標
	
	private Image playerImage;	// プレイヤーの画像
	
	private int goalWidth, goalHeight;		// ゴール画像の大きさ
	private int goalX, goalY;				// ゴールの座標
	
	private Image goalImage;		// ゴールの画像

	private final Random random = new Random();	// ランダムクラス
	
	public GamePlayPanel(JPanel parentPanel, MainPanel mPanel, int difficulty) {
		this.mPanel = mPanel;
		
		// 指定する順番のワンセットをランダムに取得
		oneset = random.nextInt(Tiles.values().length - 2) + 3;
		
		// マップを取得
		map = new Maze(difficulty, difficulty, oneset).getMap();
		// マップサイズを設定
		mapSize = (int)(parentPanel.getHeight() * 0.8);
		
		trodTiles = new int[map.length][map.length];
		
		// 上下余白のサイズを設定
		topBottomSpace = (parentPanel.getHeight() - mapSize) / 2;
		
		// タイルを描画する大きさを設定
		tileDrawsize = mapSize / difficulty;
		
		// パネルの推奨サイズを設定
		this.setPreferredSize(new Dimension(tileDrawsize * map.length, parentPanel.getHeight()));
		
		// ワンセットのタイルをランダムに決定、画像を取得する
		List<Tiles> tiles = Arrays.asList(Tiles.values());
		Collections.shuffle(tiles);
		
		scaledTileImages = new Image[oneset];
		for(int i = 0; i < oneset; i++) {
			URL imgpath = getClass().getResource("../assets/" + tiles.get(i).name() + ".png");
			try {
				BufferedImage image = ImageIO.read(imgpath);
				scaledTileImages[i] = image.getScaledInstance(tileDrawsize, tileDrawsize, Image.SCALE_AREA_AVERAGING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// マップをタイル画像に置き換える
		mapTiles = new Image[difficulty][difficulty];
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				// その座標がダミーならワンセットからランダムに選び、タイル画像とダミー番号を置き換える
				int randomNum = random.nextInt(oneset);
				mapTiles[i][j] = scaledTileImages[(map[i][j] == Maze.DUMMY) ? randomNum : map[i][j]];
				if(map[i][j] == Maze.DUMMY) map[i][j] = randomNum;
			}
		}
		
		// プレイヤー画像を読み込む
		try {
			BufferedImage playerImage = ImageIO.read(getClass().getResource(PLAYERIMAGE_PATH));
			
			// プレイヤーの大きさを設定
			double drawRatio = 0.8;	// タイルに対する描画比率
			if(topBottomSpace <= tileDrawsize) {
				playerHeight = (int)(topBottomSpace * drawRatio);
			} else {
				playerHeight = (int)(tileDrawsize * drawRatio);
			}
			playerWidth = (int)(playerImage.getWidth() * playerHeight / playerImage.getHeight());
			
			this.playerImage = playerImage.getScaledInstance(playerWidth, playerHeight, Image.SCALE_AREA_AVERAGING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// プレイヤーの初期位置を設定
		playerX = TOUtils.horizonalCentering(mapSize, playerWidth);
		playerY = topBottomSpace + mapSize + 5;
		
		// 直前に踏んだタイルを初期化
		lastTrod = oneset - 1;
		
		// ゴール画像を読み込む
		try {
			BufferedImage goalImage = ImageIO.read(getClass().getResource(GOALIMAGE_PATH));
			
			goalWidth = goalImage.getWidth() * topBottomSpace / goalImage.getHeight();
			goalHeight = topBottomSpace;
			this.goalImage = goalImage.getScaledInstance(goalWidth, goalHeight, Image.SCALE_AREA_AVERAGING);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// ゴール画像の座標を設定
		goalX = TOUtils.horizonalCentering(mapSize, goalWidth);
		goalY = 0;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				// タイルを描画する
				g2.drawImage(mapTiles[i][j], tileDrawsize * j, topBottomSpace + tileDrawsize * i, this);
				
				// タイルとプレイヤーの重なりを確認し、踏んだタイルリストへの追加とカウントを行う
				if( isTrod(tileDrawsize * j, topBottomSpace + tileDrawsize * i, tileDrawsize, tileDrawsize) ) {
					trodTiles[i][j] = 1;
					nowTread.add(map[i][j]);
				}
				
				// 踏まれているタイルは黒く描画する
				if(trodTiles[i][j] == 1) {
					g2.setColor(new Color(0, 0, 0, 200));
					g2.fillRect(tileDrawsize * j, topBottomSpace + tileDrawsize * i, tileDrawsize, tileDrawsize);
				}
			}
		}
		
		// 踏んでいるタイルが１つのとき
		if(nowTread.size() == 1) {
			lastTrod = nowTread.get(0);
		
		// 踏んでいるタイルが２つのとき
		} else if(nowTread.size() == 2) {
			int newTread = -1;	// 今新しく踏んだタイル
			for(int i = 0; i < nowTread.size(); i++) {
				if(nowTread.get(i) != lastTrod) {
					newTread = nowTread.get(i);
					break;
				}
			}
			
			// 直前に踏んだタイルと連番になっていなければゲームオーバー
			if(!(newTread == lastTrod + 1  ||  newTread == 0 && lastTrod == oneset - 1)) {
				mPanel.switchGameOverPanel(0);
			}
		}
		
		// 踏んでいるタイルのリストを初期化
		nowTread.clear();
		
		// プレイヤーを描画する
		g2.drawImage(playerImage, playerX, playerY, this);
		
		// ゴールを描画する
		g2.drawImage(goalImage, goalX, goalY, this);
	}
	
	/**
	 * タイルとプレイヤーの重なりを判定する
	 * @param tileX			タイルのX座標
	 * @param tileY			タイルのY座標
	 * @param tileWidth	タイルの横幅
	 * @param tileHeight	タイルの縦幅
	 * @return	重なっているならばtrue, そうでなければfalseを返す
	 */
	private boolean isTrod(int tileX, int tileY, int tileWidth, int tileHeight) {
		Rectangle playerRect = new Rectangle(playerX, playerY, playerWidth, playerHeight);
		Rectangle tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
		
		return playerRect.intersects(tileRect);
	}
	
	/**
	 * プレイヤーを移動させる
	 * @param speed		プレイヤーの移動速度
	 * @param vectorX	動かすX成分
	 * @param vectorY	動かすY成分
	 */
	public void movePlayer(int speed, int vectorX, int vectorY) {
		playerX += vectorX * speed;
		playerY += vectorY * speed;
	}
	
	/**
	 * 指定された順番のワンセットの数を返す
	 * @return ワンセットの数
	 */
	public int getOneset() {
		return oneset;
	}
	
	/**
	 * ワンセットの画像の配列を返す
	 * @return ワンセットの画像の配列
	 */
	public Image[] getOnesetTileImages() {
		return scaledTileImages;
	}
	
	/**
	 * このパネルでのタイルの描画サイズを返す
	 * @return　タイルの描画サイズ
	 */
	public int getTileDrawsize() {
		return tileDrawsize;
	}
	
	/**
	 * プレイヤーの座標を返す
	 * @return	プレイヤーの座標
	 */
	public Point getPlayerRelativeLocation() {
		return new Point(playerX, playerY);
	}
	
	/**
	 * プレイヤーの横幅を返す
	 * @return　プレイヤーの横幅
	 */
	public int getPlayerWidth() {
		return playerWidth;
	}
	
	/**
	 * プレイヤーの縦幅を返す
	 * @return プレイヤーの縦幅
	 */
	public int getPlayerHeight() {
		return playerHeight;
	}
	
	/**
	 * マップの上下余白の大きさを返す
	 * @return マップの上下余白の大きさ
	 */
	public int getTopBottomSpace() {
		return topBottomSpace;
	}
}
