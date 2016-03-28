import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;

public class Main {
	public static final void main(String args[]) {
		new Main().solve();
	}

	void solve() {
		try (Scanner sc = new Scanner(System.in)) {
			//初めにＡＩの名前を出力する
			System.out.println("discoNekoAI");
			System.out.flush();
			while (true) {
				System.out.print(think(sc));
				System.out.flush();
			}
		}
	}

	//
	//＊＊＊　AI方針　＊＊＊
	//・最短でソウルを目指す。
	//・リスキームーブはなるべく回避する。
	//・忍者の位置を分散させる。
	//・詰みそうな相手を詰ませる。
	//・リスキームーブを回避する相手には攻撃しない。
	//
	//＊＊＊　実際のAI挙動　＊＊＊
	//・1ターン読み
	//・分身無しルート、分身有りルートを探索して評価値の高い方を選ぶ
	//・よりソウルに近い忍者をリーダー、他をサブとする。リーダーの狙うソウル、そのソウルから最も近いソウルをサブは狙わない。
	//・犬が隣接マスにいるときは落石を前提に行動する
	//・負けそう　or　コストの元が取れそうな時は自分身
	//・分身の位置は、移動先地点の四方 > フィールド四隅　> 犬の召喚地点 > 自身の四方周囲　> ランダム　で検索
	//・敵が詰みそうなら敵落石
	//・敵が事故死しそうなら敵落石
	//・落石が効かないときは敵分身に切り替える
	//・敵がリスキームーブでソウルを取りそうなら敵分身
	//・ある程度攻撃してもゲームが終わらない場合は攻撃を止める
	//
	//＊＊＊　未実装　＊＊＊
	//・超高速
	//・回転斬り
	//・自雷撃
	//・chokudaiサーチ
	//

	private static final int dx[] = { 0, 1, 0, -1 };
	private static final int dy[] = { 1, 0, -1, 0 };
	private static final String ds[] = { "L", "U", "R", "D" };
	//ソウルの評価点
	final int soul_value = 1000;
	//ソウル数、マップの縦横
	int point, map_row, map_col;
	//最適ルートの評価点
	int x_opti = 0,x_decoy = 0;
	int lead = 0;
	int[][] route_sum;
	int[] soul_r_next, soul_c_next;
	int[] cx, cy, defp, skillCost;
	int[][] su,sd,sl,sr;
	int[][] dog_su,dog_sd,dog_sl,dog_sr;
	int[][] dogIdMap;
	int[][][] initDist,nextDist;
	boolean[][] map, itemMap, dogMap, dogAlertMap, eitemMap, edogMap, edogAlertMap;
	boolean[][] au,ad,al,ar;
	boolean[][] dog_u,dog_d,dog_l,dog_r;
	boolean[][] mru,mrd,mrl,mrr;
	boolean[] rock_exist;
	boolean flag_espell,flag_rock,flag_edecoy,flag_gendecoy,flag_decoy,flag_decoy_lose,flag_decoy_soul;
	boolean flag_alert,flag_done_serch;
	String order_str0;


	String think(Scanner sc) {
		StringBuilder res = new StringBuilder();
		//移動可能かどうかの判定
		au = new boolean [2][5];//uu,ud,ul,ur,u
		ad = new boolean [2][5];//du,dd,dl,dr,d
		al = new boolean [2][5];//lu,ld,ll,lr,l
		ar = new boolean [2][5];//ru,rd,rl,rr,r

		//移動時に岩を動かしたかどうか
		mru = new boolean [2][5];//uu,ud,ul,ur,u
		mrd = new boolean [2][5];//du,dd,dl,dr,d
		mrl = new boolean [2][5];//lu,ld,ll,lr,l
		mrr = new boolean [2][5];//ru,rd,rl,rr,r

		//自分身を使わないときの評価点
		su = new int [2][5];//uu,ud,ul,ur,u
		sd = new int [2][5];//du,dd,dl,dr,d
		sl = new int [2][5];//lu,ld,ll,lr,l
		sr = new int [2][5];//ru,rd,rl,rr,r

		//移動可能かどうかの判定
		dog_u = new boolean [2][5];//uu,ud,ul,ur,u
		dog_d = new boolean [2][5];//du,dd,dl,dr,d
		dog_l = new boolean [2][5];//lu,ld,ll,lr,l
		dog_r = new boolean [2][5];//ru,rd,rl,rr,r

		//自分身を使った時の評価点
		dog_su = new int [2][5];//uu,ud,ul,ur,u
		dog_sd = new int [2][5];//du,dd,dl,dr,d
		dog_sl = new int [2][5];//lu,ld,ll,lr,l
		dog_sr = new int [2][5];//ru,rd,rl,rr,r

		route_sum = new int [2][4];
		for(int i = 0; i < 4; i++){
			route_sum[0][i] = 0;
			route_sum[1][i] = 0;
		}
		rock_exist = new boolean [2];
		for(int i = 0; i < 2; i++){
			rock_exist[i] = false;
		}
		soul_r_next = new int [4];
		soul_c_next = new int [4];
		for(int i = 0; i < 4; i++){
			soul_r_next[i] = Integer.MAX_VALUE;
			soul_c_next[i] = Integer.MAX_VALUE;
		}

		//ここから入力の読み取り
		long millitime = sc.nextLong();
		int skills = Integer.parseInt(sc.next());
		skillCost = new int[skills];
		for (int i = 0; i < skillCost.length; ++i) {
			skillCost[i] = Integer.parseInt(sc.next());
		}
		{
			point = Integer.parseInt(sc.next());
			//System.err.print("[ally] pts : "+point);
			//point => ninja soul count
			map_row = Integer.parseInt(sc.next());
			map_col = Integer.parseInt(sc.next());
			int n;
			map = new boolean[map_row][map_col];
			for (int r = 0; r < map_row; ++r) {
				String line = sc.next();
				for (int c = 0; c < map_col; ++c) {
					map[r][c] = line.charAt(c) == '_';
				}
			}

			// character
			n = Integer.parseInt(sc.next());
			//n => ninja count 
			int rows[] = new int[n];
			int cols[] = new int[n];
			cx = new int[n];
			cy = new int[n];
			defp = new int[n];
			for (int i = 0; i < n; ++i) {
				int id = Integer.parseInt(sc.next()), row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				rows[i] = row;
				cols[i] = col;
				cx[i] = row;
				cy[i] = col;
			}
			// zombie
			dogMap = new boolean[map_row][map_col];
			dogAlertMap = new boolean[map_row][map_col];
			dogIdMap = new int[map_row][map_col];
			n = Integer.parseInt(sc.next());
			System.err.println("[ally]dogs : "+n);
			for (int i = 0; i < n; ++i) {
				int id = Integer.parseInt(sc.next()), row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				dogIdMap[row][col] = id;
				dogMap[row][col] = true;
				dogAlertMap[row][col] = true;
				dogAlertMap[row-1][col] = true;
				dogAlertMap[row+1][col] = true;
				dogAlertMap[row][col-1] = true;
				dogAlertMap[row][col+1] = true;
			}
			// item
			itemMap = new boolean[map_row][map_col];
			n = Integer.parseInt(sc.next());
			for (int i = 0; i < n; ++i) {
				int row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				itemMap[row][col] = true;
			}
			int useSkill[] = new int[skills];
			for (int i = 0; i < skills; ++i) {
				useSkill[i] = Integer.parseInt(sc.next());
			}

			lead = initDistMap();
			defp[0] = cal_soul_dist(0,cx[0],cy[0],map,itemMap);
			defp[1] = cal_soul_dist(1,cx[1],cy[1],map,itemMap);

			if(point>=skillCost[2] && useSkill[2]<2 && skillCost[2]<6)flag_rock=true;
			if(point>=skillCost[5])flag_decoy=true;
			if(point>=skillCost[6] && !flag_rock && useSkill[6]<5)flag_edecoy=true;
			if(point>=skillCost[6] && useSkill[6]<5)flag_gendecoy=true;

			order(0,rows[0], cols[0]);
			order(1,rows[1], cols[1]);
			//cal_order_score();
			order_str0 = calcRoute();
			//System.err.println(order_str0);
			String order_str_decoy = "";
			order_str_decoy = decoyRoute();
			//System.err.println(order_str_decoy);
			boolean v = false;
			boolean b = false;
			for(int i = 0; i < 2; i++){
				int sum = 0;
				for(int j = -1; j < 2; j++){
					for(int k = -1; k < 2; k++){
						if(dogMap[cx[i]+j][cy[i]+k])sum++;
					}
				}
				if(sum>0){
					int k=0;
					if(route_sum[i][0]==0)k++;
					if(route_sum[i][1]==0)k++;
					if(route_sum[i][2]==0)k++;
					if(route_sum[i][3]==0)k++;
					if(k==3 && rock_exist[i])v = true;
					//if(rock_exist[i]){System.out.println("rock on "+" k "+k);}else{System.out.println("rpck off "+" k "+k);}
					if(sum==1 && k>2)b = true;
				}
			}
			if(flag_decoy && ((x_opti==0 && x_decoy>0) || v)){
				flag_decoy_lose = true;
				flag_decoy = false;
				res.append("3").append("\n");
				res.append(order_str_decoy);
				//System.err.println("decoy_lose "+x_opti+" "+x_decoy);
			}else if(flag_decoy && x_opti<soul_value && x_decoy>soul_value && skillCost[5]<3){
				flag_decoy_soul = true;
				flag_decoy = false;
				res.append("3").append("\n");
				res.append(order_str_decoy);
				//System.err.println("decoy_soul1");
			}else if(flag_decoy && x_decoy-soul_value/2>x_opti && x_decoy>soul_value*2){
				//System.err.println(x_decoy-soul_value/2+" a "+x_opti);
				flag_decoy_soul = true;
				flag_decoy = false;
				res.append("3").append("\n");
				res.append(order_str_decoy);
				//System.err.println("decoy_soul2");
			}else if(b){
				flag_decoy_soul = true;
				flag_decoy = false;
				res.append("3").append("\n");
				res.append(order_str_decoy);
				//System.err.println("decoy_soul_b");
			}else{
				res.append(rows.length).append("\n");
				res.append(order_str0);
				//System.err.println("decoy_none");
			}
		}
		{
			int point = Integer.parseInt(sc.next()), map_row = Integer.parseInt(sc.next()), map_col = Integer.parseInt(sc.next());
			//System.err.print("[enemy]pts : "+point);
			boolean map[][] = new boolean[map_row][map_col];
			for (int r = 0; r < map_row; ++r) {
				String line = sc.next();
				for (int c = 0; c < map_col; ++c) {
					map[r][c] = line.charAt(c) == '_';
				}
			}
			int erow[] = new int[2];
			int ecol[] = new int[2];
			for (int i = 0, n = Integer.parseInt(sc.next()); i < n; ++i) {
				int id = Integer.parseInt(sc.next()), row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				erow[i] = row;
				ecol[i] = col;
			}
			edogMap = new boolean[map_row][map_col];
			edogAlertMap = new boolean[map_row][map_col];
			for (int i = 0, n = Integer.parseInt(sc.next()); i < n; ++i) {
				if(i==0)System.err.println("[enemy]dogs : "+n);
				int id = Integer.parseInt(sc.next()), row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				edogMap[row][col] = true;
				edogAlertMap[row][col] = true;
				edogAlertMap[row-1][col] = true;
				edogAlertMap[row+1][col] = true;
				edogAlertMap[row][col-1] = true;
				edogAlertMap[row][col+1] = true;
			}
			eitemMap = new boolean[map_row][map_col];
			for (int i = 0, n = Integer.parseInt(sc.next()); i < n; ++i) {
				int row = Integer.parseInt(sc.next()), col = Integer.parseInt(sc.next());
				eitemMap[row][col] = true;
			}
			int useSkill[] = new int[skills];
			for (int i = 0; i < skills; ++i) {
				useSkill[i] = Integer.parseInt(sc.next());
			}

			if(flag_rock || flag_edecoy)flag_espell = true;
			String order_str_rock = "";
			if(flag_espell){
				order_str_rock = generateRock(map,eitemMap,edogMap,edogAlertMap,erow,ecol);
				if(!flag_espell && !flag_decoy_lose && !order_str_rock.equals("")){
					if(flag_decoy_soul){
						res.delete(2,res.length());
						res.append(order_str0);
						res.insert(2,order_str_rock);
					}else{
						res.setCharAt(0, '3');
						res.insert(2,order_str_rock);
					}
				}
			}
			if(order_str_rock.equals("") && flag_gendecoy){
				String order_str_gendecoy = "";
				order_str_gendecoy = generateDecoy(map,eitemMap,edogMap,edogAlertMap,erow,ecol);
				if(!flag_decoy_lose && !order_str_gendecoy.equals("")){
						if(flag_decoy_soul){
						res.delete(2,res.length());
						res.append(order_str0);
						res.insert(2,order_str_gendecoy);
					}else{
						res.setCharAt(0, '3');
						res.insert(2,order_str_gendecoy);
					}
				}
				flag_gendecoy = false;
			}

		}
		if(flag_decoy_soul)flag_decoy_soul =false;
		if(flag_decoy_lose)flag_decoy_lose =false;

		return res.toString();
	}

	String generateDecoy(final boolean[][] rMap, final boolean[][] iMap, final boolean[][] dMap, final boolean[][] daMap, final int[] row, final int[] col){
		StringBuilder res = new StringBuilder();
		boolean[][] cu_sum = new boolean [2][5];
		boolean[][] cd_sum = new boolean [2][5];
		boolean[][] cl_sum = new boolean [2][5];
		boolean[][] cr_sum = new boolean [2][5];
		for(int i = 0; i < 2; i++){
			Arrays.fill(cu_sum[i], true);
			Arrays.fill(cd_sum[i], true);
			Arrays.fill(cl_sum[i], true);
			Arrays.fill(cr_sum[i], true);
		}
		for(int n = 0; n < 2; n++){
			int ni;
			if(n==0){ni=1;}else{ni=0;}
			int d_sum = 0, d_a_sum = 0;
			for(int i = -1; i < 2; i++){
				for(int j = -1; j < 2; j++){
					if(dMap[row[n]+i][col[n]+j]){
						d_sum++;
						if(i==0 || j==0)d_a_sum++;
					}
				}
			}
			if(d_a_sum>0){
				//move abel check (rock and dogs)
				//U
				if(row[n]==1){
					Arrays.fill(cu_sum[n], false);//UU,UD,UL,UR,U
					cl_sum[n][0] = false;//LU
					cr_sum[n][0] = false;//RU
				}else if(row[n]==2){
					cu_sum[n][0] = false;//UU
					if(!rMap[row[n]-1][col[n]]){
						Arrays.fill(cu_sum[n], false);//UU,UD,UL,UR,U
					}else{//UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[n][2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[n][3] = false;//UR
					}
				}else{
					boolean check_sum = true;
					if(!rMap[row[n]-1][col[n]]){
						if(!rMap[row[n]-2][col[n]])
							check_sum = false;
						if(dMap[row[n]-2][col[n]])
							check_sum =false;
					}

					if(!check_sum){
						Arrays.fill(cu_sum[n], false);//UU,UD,UL,UR,U
					}else{//UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[n][2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[n][3] = false;//UR
					}
					if((!rMap[row[n]-1][col[n]] || !rMap[row[n]-2][col[n]]) && (!rMap[row[n]-3][col[n]] || dMap[row[n]-3][col[n]]))//UU
						cu_sum[n][0] = false;
				}
				//D
				if(row[n]==map_row-2){
					Arrays.fill(cd_sum[n], false);//DU,DD,DL,DR,D
					cl_sum[n][1] = false;//LD
					cr_sum[n][1] = false;//RD
				}else if(row[n]==map_row-3){
					cd_sum[n][1] = false;//DD
					if(!rMap[row[n]+1][col[n]]){
						Arrays.fill(cd_sum[n], false);//DU,DD,DL,DR,D
					}else{//DU,DL,DR,D
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[n][2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[n][3] = false;//DR
					}
				}else{
					boolean check_sum = true;
					if(!rMap[row[n]+1][col[n]]){
						if(!rMap[row[n]+2][col[n]])
							check_sum = false;
						if(dMap[row[n]+2][col[n]])
							check_sum =false;
					}

					if(!check_sum){
						Arrays.fill(cd_sum[n], false);//UU,UD,UL,UR,U
					}else{//DU,DL,DR,D
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[n][2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[n][3] = false;//DR
					}
					if((!rMap[row[n]+1][col[n]] || !rMap[row[n]+2][col[n]]) && (!rMap[row[n]+3][col[n]] || dMap[row[n]+3][col[n]]))
						cd_sum[n][1] = false;//DD
				}

				//L
				if(col[n]==1){
					Arrays.fill(cl_sum[n], false);
					cu_sum[n][2] = false;
					cd_sum[n][2] = false;
				}else if(col[n]==2){
					cl_sum[n][2] = false;//LL
					if(!rMap[row[n]][col[n]-1]){
						Arrays.fill(cl_sum[n], false);
					}else{//LU,LD,LR,L
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[n][0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[n][1] = false;//LD
					}
				}else{
					boolean check_sum = true;
					if(!rMap[row[n]][col[n]-1]){
						if(!rMap[row[n]][col[n]-2])
							check_sum = false;
						if(dMap[row[n]][col[n]-2])
							check_sum =false;
					}
					//if(check_sum){System.out.println("no collision true");}else{System.out.println("get collision false");}

					if(!check_sum){
						Arrays.fill(cl_sum[n], false);//LU,LD,LL,LR,L
					}else{//LU,LD,LR,L
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[n][0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[n][1] = false;//LD
					}
					if((!rMap[row[n]][col[n]-1] || !rMap[row[n]][col[n]-2]) && (!rMap[row[n]][col[n]-3] || dMap[row[n]][col[n]-3]))
						cl_sum[n][2] = false;//LL
				}

				//R
				if(col[n]==map_col-2){
					Arrays.fill(cr_sum[n], false);
					cu_sum[n][3] = false;
					cd_sum[n][3] = false;
				}else if(col[n]==map_col-3){
					cr_sum[n][3] = false;//RR
					if(!rMap[row[n]][col[n]+1]){
						Arrays.fill(cr_sum[n], false);
					}else{//RU,RD,RL,R
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[n][0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[n][1] = false;//RD
					}
				}else{
					boolean check_sum = true;
					if(!rMap[row[n]][col[n]+1]){
						if(!rMap[row[n]][col[n]+2])
							check_sum = false;
						if(dMap[row[n]][col[n]+2])
							check_sum =false;
					}

					if(!check_sum){
						Arrays.fill(cr_sum[n], false);
					}else{//RU,RD,RL,R
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[n][0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[n][1] = false;//RD
					}
					if((!rMap[row[n]][col[n]+1] || !rMap[row[n]][col[n]+2]) && (!rMap[row[n]][col[n]+3] || dMap[row[n]][col[n]+3]))
						cr_sum[n][3] = false;//RR
				}

			}else{
				Arrays.fill(cu_sum[n], false);
				Arrays.fill(cd_sum[n], false);
				Arrays.fill(cl_sum[n], false);
				Arrays.fill(cr_sum[n], false);
			}
		}
		final int sx[] = { -1, 1, 0, 0 ,0};
		final int sy[] = { 0, 0, -1, 1 ,0};
		boolean f = false;
		int r = -1, c = -1;
		int[] cnt = {0,0};
		for(int n = 0; n < 2; n++){
			if(f && cnt[0]==1)break;
			for(int i = 0; i < 4; i++){
				//if(f)break;
				if(!f && cu_sum[n][i]){
					if(row[n]-1+sx[i]>0 && col[n]+sy[i]>0 && col[n]+sy[i]<map_col-1 && iMap[row[n]-1+sx[i]][col[n]+sy[i]] && rMap[row[n]-1+sx[i]][col[n]+sy[i]]){
						f = true;
						r = row[n]-1+sx[i];
						c = col[n]+sy[i];
						cnt[n]++;
						//System.out.println(n+" U[i] "+i+" "+cnt[n]);
					}
				}
				if(!f && cd_sum[n][i]){
					if(row[n]+1+sx[i]<map_row-1 && col[n]+sy[i]>0 && col[n]+sy[i]<map_col-1 && iMap[row[n]+1+sx[i]][col[n]+sy[i]] && rMap[row[n]+1+sx[i]][col[n]+sy[i]]){
						f = true;
						r = row[n]+1+sx[i];
						c = col[n]+sy[i];
						cnt[n]++;
						//System.out.println(n+" D[i] "+i+" "+cnt[n]);
					}
				}
				if(!f && cl_sum[n][i]){
					if(col[n]-1+sy[i]>0 && row[n]+sx[i]>0 && row[n]+sx[i]<map_row-1 && iMap[row[n]+sx[i]][col[n]-1+sy[i]] && rMap[row[n]+sx[i]][col[n]-1+sy[i]]){
						f = true;
						r = row[n]+sx[i];
						c = col[n]-1+sy[i];
						cnt[n]++;
						//System.out.println(n+" L[i] "+i+" "+cnt[n]);
					}
				}
				if(!f && cr_sum[n][i]){
					if(col[n]+1+sy[i]<map_col-1 && row[n]+sx[i]>0 && row[n]+sx[i]<map_row-1 && iMap[row[n]+sx[i]][col[n]+1+sy[i]] && rMap[row[n]+sx[i]][col[n]+1+sy[i]]){
						f = true;
						r = row[n]+sx[i];
						c = col[n]+1+sy[i];
						cnt[n]++;
						//System.out.println(n+" R[i] "+i+" "+cnt[n]);
					}
				}
			}
		}
		if(f && (cnt[0]==1 || cnt[1]==1)){
			boolean v = false;
			if(dMap[r][c])v = true;
			if(r-1>0 && dMap[r-1][c])v = true;
			if(r+1<map_row-1 && dMap[r+1][c])v = true;
			if(c-1>0 && dMap[r][c-1])v = true;
			if(c+1<map_col-1 && dMap[r][c+1])v = true;
			if(v)res.append(6+" "+r+" "+c+"\n");
		}
		//System.out.println("heyhey "+cnt[0]+" next "+cnt[1]);
		String str = "";
		str = res.toString();
		return str;
	}
	
	String generateRock(final boolean[][] rMap, final boolean[][] iMap, final boolean[][] dMap, final boolean[][] daMap, final int[] row, final int[] col){
		StringBuilder res = new StringBuilder();
		int pos_r = -1, pos_c = -1;
		boolean[][] gu = new boolean[2][5];
		boolean[][] gd = new boolean[2][5];
		boolean[][] gl = new boolean[2][5];
		boolean[][] gr = new boolean[2][5];
		for(int n = 0; n < 2; n++){

			int d_sum = 0, d_a_sum = 0;
			for(int i = -1; i < 2; i++){
				for(int j = -1; j < 2; j++){
					if(dMap[row[n]+i][col[n]+j]){
						d_sum++;
						if(i==0 || j==0)d_a_sum++;
					}
				}
			}
			boolean[] cu_sum = new boolean [5];
			boolean[] cd_sum = new boolean [5];
			boolean[] cl_sum = new boolean [5];
			boolean[] cr_sum = new boolean [5];
			Arrays.fill(cu_sum, true);
			Arrays.fill(cd_sum, true);
			Arrays.fill(cl_sum, true);
			Arrays.fill(cr_sum, true);
			if(d_a_sum>0){
				//move abel check (rock and dogs)
				//U
				if(row[n]==1){
					Arrays.fill(cu_sum, false);//UU,UD,UL,UR,U
					cl_sum[0] = false;//LU
					cr_sum[0] = false;//RU
				}else if(row[n]==2){
					cu_sum[0] = false;//UU
					if(!rMap[row[n]-1][col[n]]){
						Arrays.fill(cu_sum, false);//UU,UD,UL,UR,U
					}else if(dMap[row[n]-1][col[n]]){
						Arrays.fill(cu_sum, false);//UU,UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[3] = false;//UR
					}else{//UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[3] = false;//UR
						if(daMap[row[n]][col[n]])
							cu_sum[1] = false;//UD
						if(daMap[row[n]-1][col[n]-1])
							cu_sum[2] = false;//UL
						if(daMap[row[n]-1][col[n]+1])
							cu_sum[3] = false;//UR
						if(daMap[row[n]-1][col[n]])
							cu_sum[4] = false;//U
					}
				}else{
					boolean check_sum = true;
					boolean check_d = true;
					if(!rMap[row[n]-1][col[n]]){
						if(!rMap[row[n]-2][col[n]])
							check_sum = false;
						if(dMap[row[n]-2][col[n]])
							check_sum =false;
					}
					if(dMap[row[n]-1][col[n]])check_d = false;

					if(!check_sum){
						Arrays.fill(cu_sum, false);//UU,UD,UL,UR,U
					}else if(!check_d){
						Arrays.fill(cu_sum, false);//UU,UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[3] = false;//UR
					}else{//UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]))
							cu_sum[2] = false;//UL
						if(col[n]<map_col-2 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]))
							cu_sum[3] = false;//UR
						if(daMap[row[n]][col[n]])
							cu_sum[1] = false;//UD
						if(daMap[row[n]-1][col[n]-1])
							cu_sum[2] = false;//UL
						if(daMap[row[n]-1][col[n]+1])
							cu_sum[3] = false;//UR
						if(daMap[row[n]-1][col[n]])
							cu_sum[4] = false;//U
					}
					if((!rMap[row[n]-1][col[n]] || !rMap[row[n]-2][col[n]]) && (!rMap[row[n]-3][col[n]] || dMap[row[n]-3][col[n]]))//UU
						cu_sum[0] = false;
					if(daMap[row[n]-2][col[n]])
						cu_sum[0] = false;
				}
				//D
				if(row[n]==map_row-2){
					Arrays.fill(cd_sum, false);//DU,DD,DL,DR,D
					cl_sum[1] = false;//LD
					cr_sum[1] = false;//RD
				}else if(row[n]==map_row-3){
					cd_sum[1] = false;//DD
					if(!rMap[row[n]+1][col[n]]){
						Arrays.fill(cd_sum, false);//DU,DD,DL,DR,D
					}else if(dMap[row[n]+1][col[n]]){
						Arrays.fill(cd_sum, false);//DU,DD,DL,DR,D
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[3] = false;//DR
					}else{//DU,DL,DR,D
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[3] = false;//DR
						if(daMap[row[n]][col[n]])
							cd_sum[0] = false;//DU
						if(daMap[row[n]+1][col[n]-1])
							cd_sum[2] = false;//DL
						if(daMap[row[n]+1][col[n]+1])
							cd_sum[3] = false;//DR
						if(daMap[row[n]+1][col[n]])
							cd_sum[4] = false;//D
					}
				}else{
					boolean check_sum = true;
					boolean check_d = true;
					if(!rMap[row[n]+1][col[n]]){
						if(!rMap[row[n]+2][col[n]])
							check_sum = false;
						if(dMap[row[n]+2][col[n]])
							check_sum =false;
					}
					if(dMap[row[n]+1][col[n]])check_d = false;

					if(!check_sum){
						Arrays.fill(cd_sum, false);//UU,UD,UL,UR,U
					}else if(!check_d){
						Arrays.fill(cd_sum, false);//UU,UD,UL,UR,U
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[3] = false;//DR
					}else{//DU,DL,DR,D
						if(col[n]>1 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]))
							cd_sum[2] = false;//DL
						if(col[n]<map_col-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]))
							cd_sum[3] = false;//DR
						if(daMap[row[n]][col[n]])
							cd_sum[0] = false;//DU
						if(daMap[row[n]+1][col[n]-1])
							cd_sum[2] = false;//DL
						if(daMap[row[n]+1][col[n]+1])
							cd_sum[3] = false;//DR
						if(daMap[row[n]+1][col[n]])
							cd_sum[4] = false;//D
					}
					if((!rMap[row[n]+1][col[n]] || !rMap[row[n]+2][col[n]]) && (!rMap[row[n]+3][col[n]] || dMap[row[n]+3][col[n]]))
						cd_sum[1] = false;//DD
					if(daMap[row[n]+2][col[n]])
						cd_sum[1] = false;//DD
				}

				//L
				if(col[n]==1){
					Arrays.fill(cl_sum, false);
					cu_sum[2] = false;
					cd_sum[2] = false;
				}else if(col[n]==2){
					cl_sum[2] = false;//LL
					if(!rMap[row[n]][col[n]-1]){
						Arrays.fill(cl_sum, false);
					}else if(dMap[row[n]][col[n]-1]){
						Arrays.fill(cl_sum, false);
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[1] = false;//LD
					}else{//LU,LD,LR,L
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[1] = false;//LD
						if(daMap[row[n]-1][col[n]-1])
							cl_sum[0] = false;//LU
						if(daMap[row[n]+1][col[n]-1])
							cl_sum[1] = false;//LD
						if(daMap[row[n]][col[n]])
							cl_sum[3] = false;//LR
						if(daMap[row[n]][col[n]-1])
							cl_sum[4] = false;//L
					}
				}else{
					boolean check_sum = true;
					boolean check_d = true;
					if(!rMap[row[n]][col[n]-1]){
						if(!rMap[row[n]][col[n]-2])
							check_sum = false;
						if(dMap[row[n]][col[n]-2])
							check_sum =false;
					}
					if(dMap[row[n]][col[n]-1])check_d = false;

					if(!check_sum){
						Arrays.fill(cl_sum, false);//LU,LD,LL,LR,L
					}else if(!check_d){
						Arrays.fill(cl_sum, false);//LU,LD,LL,LR,L
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[1] = false;//LD
					}else{//LU,LD,LR,L
						if(row[n]>1 && !rMap[row[n]-1][col[n]-1] && (!rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]))
							cl_sum[0] = false;//LU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]-1] && (!rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]))
							cl_sum[1] = false;//LD
						if(daMap[row[n]-1][col[n]-1])
							cl_sum[0] = false;//LU
						if(daMap[row[n]+1][col[n]-1])
							cl_sum[1] = false;//LD
						if(daMap[row[n]][col[n]])
							cl_sum[3] = false;//LR
						if(daMap[row[n]][col[n]-1])
							cl_sum[4] = false;//L
					}
					if((!rMap[row[n]][col[n]-1] || !rMap[row[n]][col[n]-2]) && (!rMap[row[n]][col[n]-3] || dMap[row[n]][col[n]-3]))
						cl_sum[2] = false;//LL
					if(daMap[row[n]][col[n]-2])
						cl_sum[2] = false;//LL
				}

				//R
				if(col[n]==map_col-2){
					Arrays.fill(cr_sum, false);
					cu_sum[3] = false;
					cd_sum[3] = false;
				}else if(col[n]==map_col-3){
					cr_sum[3] = false;//RR
					if(!rMap[row[n]][col[n]+1]){
						Arrays.fill(cr_sum, false);
					}else if(dMap[row[n]][col[n]+1]){
						Arrays.fill(cr_sum, false);
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[1] = false;//RD
					}else{//RU,RD,RL,R
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[1] = false;//RD
						if(daMap[row[n]-1][col[n]+1])
							cr_sum[0] = false;//RU
						if(daMap[row[n]+1][col[n]+1])
							cr_sum[1] = false;//RD
						if(daMap[row[n]][col[n]])
							cr_sum[2] = false;//RL
						if(daMap[row[n]][col[n]+1])
							cr_sum[4] = false;//R
					}
				}else{
					boolean check_sum = true;
					boolean check_d = true;
					if(!rMap[row[n]][col[n]+1]){
						if(!rMap[row[n]][col[n]+2])
							check_sum = false;
						if(dMap[row[n]][col[n]+2])
							check_sum =false;
					}
					if(dMap[row[n]][col[n]+1])check_d = false;

					if(!check_sum){
						Arrays.fill(cr_sum, false);
					}else if(!check_d){
						Arrays.fill(cr_sum, false);
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[1] = false;//RD
					}else{//RU,RD,RL,R
						if(row[n]>1 && !rMap[row[n]-1][col[n]+1] && (!rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]))
							cr_sum[0] = false;//RU
						if(row[n]<map_row-2 && !rMap[row[n]+1][col[n]+1] && (!rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]))
							cr_sum[1] = false;//RD
						if(daMap[row[n]-1][col[n]+1])
							cr_sum[0] = false;//RU
						if(daMap[row[n]+1][col[n]+1])
							cr_sum[1] = false;//RD
						if(daMap[row[n]][col[n]])
							cr_sum[2] = false;//RL
						if(daMap[row[n]][col[n]+1])
							cr_sum[4] = false;//R
					}
					if((!rMap[row[n]][col[n]+1] || !rMap[row[n]][col[n]+2]) && (!rMap[row[n]][col[n]+3] || dMap[row[n]][col[n]+3]))
						cr_sum[3] = false;//RR
					if(daMap[row[n]][col[n]+2])
						cr_sum[3] = false;//RR
				}

				int mcnt = 0;
				for(int i = 0; i < 5; i++){
					if(i!=0 && cu_sum[i])mcnt++;
					if(i!=1 && cd_sum[i])mcnt++;
					if(i!=2 && cl_sum[i])mcnt++;
					if(i!=3 && cr_sum[i])mcnt++;
				}
				//System.err.println("mcnt;"+mcnt);
				if(mcnt==1){
					if(row[n]-2>0 && cu_sum[0] && !iMap[row[n]-2][col[n]]){//UU
						if(row[n]-3>0 && !rMap[row[n]-1][col[n]] || !rMap[row[n]-3][col[n]] || dMap[row[n]-3][col[n]]){
							pos_r = row[n]-2; pos_c = col[n]; flag_espell = false;
						}
					}else if(row[n]+2<map_row-1 && cd_sum[0] && !iMap[row[n]+2][col[n]]){//DD
						if(row[n]+3<map_row-1 && !rMap[row[n]+1][col[n]] || !rMap[row[n]+3][col[n]] || dMap[row[n]+3][col[n]]){
							pos_r = row[n]+2; pos_c = col[n]; flag_espell = false;
						}
					}else if(col[n]-2>0 && cl_sum[0] && !iMap[row[n]][col[n]-2]){//LL
						if(col[n]-3>0 && !rMap[row[n]][col[n]-1] || !rMap[row[n]][col[n]-3] || dMap[row[n]][col[n]-3]){
							pos_r = row[n]; pos_c = col[n]-2; flag_espell = false;
						}
					}else if(col[n]+2<map_col-1 && cr_sum[0] && !iMap[row[n]][col[n]+2]){//RR
						if(col[n]+3<map_col-1 && !rMap[row[n]][col[n]+1] || !rMap[row[n]][col[n]+3] || dMap[row[n]][col[n]+3]){
							pos_r = row[n]; pos_c = col[n]+2; flag_espell = false;
						}
					}else if(cu_sum[4] && !iMap[row[n]-1][col[n]]){//U
						if(row[n]-2>0 && !rMap[row[n]-2][col[n]] || dMap[row[n]-2][col[n]]){
							pos_r = row[n]-1; pos_c = col[n]; flag_espell = false;
						}
					}else if(cd_sum[4] && !iMap[row[n]+1][col[n]]){//D
						if(row[n]+2<map_row-1 && !rMap[row[n]+2][col[n]] || dMap[row[n]+2][col[n]]){
							pos_r = row[n]+1; pos_c = col[n]; flag_espell = false;
						}
					}else if(cl_sum[4] && !iMap[row[n]][col[n]-1]){//L
						if(col[n]-2>0 && !rMap[row[n]][col[n]-2] || dMap[row[n]][col[n]-2]){
							pos_r = row[n]; pos_c = col[n]-1; flag_espell = false;
						}
					}else if(cr_sum[4] && !iMap[row[n]][col[n]+1]){//R
						if(col[n]+2<map_col-1 && !rMap[row[n]][col[n]+2] || dMap[row[n]][col[n]+2]){
							pos_r = row[n]; pos_c = col[n]+1; flag_espell = false;
						}
					}else if(cu_sum[2] && !iMap[row[n]-1][col[n]-1]){//UL
						if(col[n]-2>0 && !rMap[row[n]-1][col[n]-2] || dMap[row[n]-1][col[n]-2]){
							pos_r = row[n]-1; pos_c = col[n]-1; flag_espell = false;
						}
					}else if(cu_sum[3] && !iMap[row[n]-1][col[n]+1]){//UR
						if(col[n]+2<map_col-1 && !rMap[row[n]-1][col[n]+2] || dMap[row[n]-1][col[n]+2]){
							pos_r = row[n]-1; pos_c = col[n]+1; flag_espell = false;
						}
					}else if(cd_sum[2] && !iMap[row[n]+1][col[n]-1]){//DL
						if(col[n]-2>0 && !rMap[row[n]+1][col[n]-2] || dMap[row[n]+1][col[n]-2]){
							pos_r = row[n]+1; pos_c = col[n]-1; flag_espell = false;
						}
					}else if(cd_sum[3] && !iMap[row[n]+1][col[n]+1]){//DR
						if(col[n]+2<map_col-1 && !rMap[row[n]+1][col[n]+2] || dMap[row[n]+1][col[n]+2]){
							pos_r = row[n]+1; pos_c = col[n]+1; flag_espell = false;
						}
					}else if(cl_sum[0] && !iMap[row[n]-1][col[n]-1]){//LU
						if(row[n]-2>0 && !rMap[row[n]-2][col[n]-1] || dMap[row[n]-2][col[n]-1]){
							pos_r = row[n]-1; pos_c = col[n]-1; flag_espell = false;
						}
					}else if(cl_sum[1] && !iMap[row[n]+1][col[n]-1]){//LD
						if(row[n]+2<map_row-1 && !rMap[row[n]+2][col[n]-1] || dMap[row[n]+2][col[n]-1]){
							pos_r = row[n]+1; pos_c = col[n]-1; flag_espell = false;
						}
					}else if(cr_sum[0] && !iMap[row[n]-1][col[n]+1]){//RU
						if(row[n]-2>0 && !rMap[row[n]-2][col[n]+1] || dMap[row[n]-2][col[n]+1]){
							pos_r = row[n]-1; pos_c = col[n]+1; flag_espell = false;
						}
					}else if(cr_sum[1] && !iMap[row[n]+1][col[n]+1]){//RD
						if(row[n]+2<map_row-1 && !rMap[row[n]+2][col[n]+1] || dMap[row[n]+2][col[n]+1]){
							pos_r = row[n]+1; pos_c = col[n]+1; flag_espell = false;
						}
					}
					if((pos_r==row[0] && pos_c==col[0]) || (pos_r==row[1] && pos_c==col[1])){
						flag_espell = true;
					}
				}
			}

			// --- rock set position[kill] ---
			if(flag_espell && d_a_sum>0){
				int check_c = 0;
				boolean[] check = new boolean [4];
				boolean esc = false;					
				for(int i = 0; i < 4; i++){
					int r1 = row[n]+dy[i], c1 = col[n]+dx[i];
					int r2 = r1+dy[i], c2 = c1+dx[i];
					if(dy[i]!=0 && (r1==0 || r1==map_row-1)){
						check[i] = true;
						check_c++;
					}else if(dy[i]!=0 && (r2==0 || r2==map_row-1)){
						if(!rMap[r1][c1] || dMap[r1][c1]){
							check[i] = true;
							check_c++;
						}
					}else if(dy[i]!=0){
						int o_sum = 0;
						if(!rMap[r1][c1])o_sum++;
						if(!rMap[r2][c2])o_sum++;
						if(dMap[r1][c1])o_sum+=2;
						if(dMap[r2][c2])o_sum++;
						if(o_sum==0)esc = true;
						if(o_sum>1){
							check[i] = true;
							check_c++;
						}
					}
					if(dx[i]!=0 && (c1==0 || c1==map_col-1)){
						check[i] = true;
						check_c++;
					}else if(dx[i]!=0 && (c2==0 || c2==map_col-1)){
						if(!rMap[r1][c1] || dMap[r1][c1]){
							check[i] = true;
							check_c++;
						}
					}else if(dx[i]!=0){
						int o_sum = 0;
						if(!rMap[r1][c1])o_sum++;
						if(!rMap[r2][c2])o_sum++;
						if(dMap[r1][c1])o_sum+=2;
						if(dMap[r2][c2])o_sum++;
						if(o_sum==0)esc = true;
						if(o_sum>1){
							check[i] = true;
							check_c++;
						}
					}
					if(esc)break;
				}
				if(!esc && check_c==3){
					for(int i = 0; i < 4; i++){
						if(!check[i]){
							int r1 = row[n]+dy[i], c1 = col[n]+dx[i];
							int r2 = r1+dy[i], c2 = c1+dx[i];
							if(rMap[r1][c1] && !iMap[r1][c1]){
								if(flag_rock){
									pos_r = r1; pos_c = c1;
									flag_espell = false;
									if((pos_r==row[0] && pos_c==col[0]) || (pos_r==row[1] && pos_c==col[1])){
										flag_espell = true;
									}
								}else if(flag_edecoy){
									pos_r = row[n]; pos_c = col[n];
									flag_espell = false;
								}
							}else if(rMap[r2][c2] && !iMap[r2][c2]){
								if(flag_rock){
									pos_r = r2; pos_c = c2;
									flag_espell = false;
									if((pos_r==row[0] && pos_c==col[0]) || (pos_r==row[1] && pos_c==col[1])){
										flag_espell = true;
									}
								}else if(flag_edecoy){
									pos_r = row[n]; pos_c = col[n];
									flag_espell = false;
								}
							}
						}
					}
				}
					// --- rock set position[jammer moving when near dogs and soul] ---
				if(d_sum>0){
					for(int i = 0; i < 4; i++){
						if(!check[i]){
							int r1 = row[n]+dy[i], c1 = col[n]+dx[i];
							int r2 = r1+dy[i], c2 = c1+dx[i];
							if(dy[i]!=0 && (r2<0 || map_row-1<r2))continue;
							if(dx[i]!=0 && (c2<0 || map_col-1<c2))continue;
							if(!dMap[r1][c1] && !dMap[r2][c2]){
								if(!rMap[r1][c1] && rMap[r2][c2] && iMap[r1][c1] && !iMap[r2][c2]){
									if(flag_rock){
										pos_r = r2; pos_c = c2;
										flag_espell = false;
										if((pos_r==row[0] && pos_c==col[0]) || (pos_r==row[1] && pos_c==col[1])){
											flag_espell = true;
										}
									}
								}
							}
						}
					}
				}
				/*
					// --- rock set position[jammer moving when near dogs] ---
				if(check_c==2){
					for(int i = 0; i < 4; i++){
						if(!check[i]){
							int r1 = row[n]+dy[i], c1 = col[n]+dx[i];
							int r2 = r1+dy[i], c2 = c1+dx[i];
							if(dy[i]!=0 && (r2<0 || map_row-1<r2))continue;
							if(dx[i]!=0 && (c2<0 || map_col-1<c2))continue;
							if(!dMap[r1][c1] && !dMap[r2][c2]){
								if(rMap[r1][c1] && !rMap[r2][c2] && !iMap[r1][c1]){
									pos_r = r1; pos_c = c1; flag_rock = false;
								}else if(!rMap[r1][c1] && rMap[r2][c2] && !iMap[r2][c2]){
									pos_r = r2; pos_c = c2; flag_rock = false;
								}
							}
						}
					}
				}
				*/
			}
			if(!flag_espell)break;
		}
		if(!flag_espell){
			if(flag_rock){
				res.append(2+" "+pos_r+" "+pos_c+"\n");
				flag_rock = false;
			}else if(flag_edecoy){
				res.append(6+" "+pos_r+" "+pos_c+"\n");
				flag_edecoy = false;
			}
		}
		return res.toString();
	}



	String calcRoute(){
		//System.out.println("cal_route s");
		boolean[][] fixDogMap = new boolean [map_row][map_col];
		boolean[][] fixMap = new boolean [map_row][map_col];
		String route ="";
		int[] sx = {-1,1,0,0,0};
		int[] sy = {0,0,-1,1,0};
		int[] pr = {cx[0],cx[1]};
		int[] pc = {cy[0],cy[1]};
		String[] str = {"U","D","L","R","N"};
		String[] moves = new String [4];
		int s0,s1;
		int max_s,xscore,x0,x1,xmr_cnt;
		boolean[] xrock_exist = new boolean[]{false,false};
		boolean mr_check0,mr_check1,xcv;
		String[] opti_moves = new String [4];
		xscore = 0;
		xmr_cnt = 0;
		x0 = 0;
		x1 = 0;
		moves[0] = "N";
		moves[1] = "N";
		moves[2] = "N";
		moves[3] = "N";
		while(true){
			mr_check0 = false;
			mr_check1 = false;
			max_s = 0;

			for(int i = 0; i < 5; i++){
				if(au[0][i] && dog_u[0][i]){
					if(su[0][i]>max_s || (su[0][i]==max_s && mr_check0 && !mru[0][i])){
						max_s = su[0][i];
						moves[0] = "U";
						moves[1] = str[i];
						pr[0] = cx[0] + sx[i] -1;
						pc[0] = cy[0] + sy[i];
						if(mru[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(ad[0][i] && dog_d[0][i]){
					if(sd[0][i]>max_s || (sd[0][i]==max_s && mr_check0 && !mrd[0][i])){
						max_s = sd[0][i];
						moves[0] = "D";
						moves[1] = str[i];
						pr[0] = cx[0] + sx[i] +1;
						pc[0] = cy[0] + sy[i];
						if(mrd[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(al[0][i] && dog_l[0][i]){
					if(sl[0][i]>max_s || (sl[0][i]==max_s && mr_check0 && !mrl[0][i])){
						max_s = sl[0][i];
						moves[0] = "L";
						moves[1] = str[i];
						pr[0] = cx[0] + sx[i];
						pc[0] = cy[0] + sy[i] -1;
						if(mrl[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(ar[0][i] && dog_r[0][i]){
					if(sr[0][i]>max_s || (sr[0][i]==max_s && mr_check0 && !mrr[0][i])){
						max_s = sr[0][i];
						moves[0] = "R";
						moves[1] = str[i];
						pr[0] = cx[0] + sx[i];
						pc[0] = cy[0] + sy[i] +1;
						if(mrr[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
			}
			if(x0<max_s)x0=max_s;
			s0 = max_s;
			if(max_s==0)break;
			for(int i = 0; i < 5; i++){
				switch(moves[0]){
					case "U": 
					if(moves[1].equals(str[i])){
						su[0][i] = 0;
					}
					break;
					case "D": 
					if(moves[1].equals(str[i])){
						sd[0][i] = 0;
					}
					break;
					case "L": 
					if(moves[1].equals(str[i])){
						sl[0][i] = 0;
					}
					break;
					case "R": 
					if(moves[1].equals(str[i])){
						sr[0][i] = 0;
					}
					break;
					}
			}
			String[] temp_moves = {moves[0],moves[1],"",""};
			boolean[] temp_re = new boolean[]{false,false};
			boolean cv,mcv;
			int danger;
			max_s = 0;
			mcv = true;
			pr[1] = -1; pc[1] = -1;
			int[] ads = new int[]{0,0};
			for(int i = 0; i < 2; i++){
				if(dogMap[cx[i]-1][cy[i]])ads[i]++;
				if(dogMap[cx[i]+1][cy[i]])ads[i]++;
				if(dogMap[cx[i]][cy[i]-1])ads[i]++;
				if(dogMap[cx[i]][cy[i]+1])ads[i]++;
			}
	
			for(int i = 0; i < 5; i++){
				cv = alertCheckDist(map,dogMap,cx[1],cy[1],-1,0);
				if(cv){danger = 0;}else{danger = 1;}
				if(au[1][i] && dog_u[1][i] && (su[1][i]-danger>max_s || (ads[1]>0 && su[1][i]-danger>max_s-1 && mr_check1 && !mru[1][i]))){
					temp_moves[2] = "U";
					temp_moves[3] = str[i];
					fixMap = cal_rock_map(cx,cy,temp_moves,map);
					if(!fixMap[0][0]){
						pr[1] = cx[1] + sx[i] -1;
						pc[1] = cy[1] + sy[i];
						fixDogMap = dog_moves2(pr,pc,fixMap);
						boolean dv = alertCheckRock(map,dogMap,cx[1],cy[1],sx[i],sy[i]);
						boolean v = alertCheck(fixMap,fixDogMap,pr,pc);
						if(!dv && !cv)v=false;
						if(v && !fixDogMap[pr[0]][pc[0]] && !fixDogMap[pr[1]][pc[1]]){
							max_s = su[1][i];
							moves[2] = "U";
							moves[3] = str[i];
							mcv = cv;
							if(mru[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							temp_re = new boolean[]{false,false};
							for(int n = 0; n < 2; n++){
								if(route_sum[n][0]>0){
									if(!fixMap[pr[n]-1][pc[n]] || (pr[n]-2>=0 && !fixMap[pr[n]-2][pc[n]])){
										temp_re[n] = true;
									}
								}
							}
						}
					}
				}
				cv = alertCheckDist(map,dogMap,cx[1],cy[1],1,0);
				if(cv){danger = 0;}else{danger = 1;}
				if(ad[1][i] && dog_d[1][i] && (sd[1][i]-danger>max_s || (ads[1]>0 && sd[1][i]-danger>max_s-1 && mr_check1 && !mrd[1][i]))){
					temp_moves[2] = "D";
					temp_moves[3] = str[i];
					fixMap = cal_rock_map(cx,cy,temp_moves,map);
					if(!fixMap[0][0]){
						pr[1] = cx[1] + sx[i] +1;
						pc[1] = cy[1] + sy[i];
						fixDogMap = dog_moves2(pr,pc,fixMap);
						boolean dv = alertCheckRock(map,dogMap,cx[1],cy[1],sx[i],sy[i]);
						boolean v = alertCheck(fixMap,fixDogMap,pr,pc);
						if(!dv && !cv)v=false;
						if(v && !fixDogMap[pr[0]][pc[0]] && !fixDogMap[pr[1]][pc[1]]){
							max_s = sd[1][i];
							moves[2] = "D";
							moves[3] = str[i];
							mcv = cv;
							if(mrd[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							temp_re = new boolean[]{false,false};
							for(int n = 0; n < 2; n++){
								if(route_sum[n][1]>0){
									if(!fixMap[pr[n]+1][pc[n]] || (pr[n]+2<=map_row-1 && !fixMap[pr[n]+2][pc[n]])){
										temp_re[n] = true;
									}
								}
							}
						}
					}
				}
				
				cv = alertCheckDist(map,dogMap,cx[1],cy[1],0,-1);
				if(cv){danger = 0;}else{danger = 1;}
				if(al[1][i] && dog_l[1][i] && (sl[1][i]-danger>max_s || (ads[1]>0 && sl[1][i]-danger>max_s-1 && mr_check1 && !mrl[1][i]))){
					temp_moves[2] = "L";
					temp_moves[3] = str[i];
					fixMap = cal_rock_map(cx,cy,temp_moves,map);
					if(!fixMap[0][0]){
						pr[1] = cx[1] + sx[i];
						pc[1] = cy[1] + sy[i] -1;
						fixDogMap = dog_moves2(pr,pc,fixMap);
						boolean dv = alertCheckRock(map,dogMap,cx[1],cy[1],sx[i],sy[i]);
						boolean v = alertCheck(fixMap,fixDogMap,pr,pc);
						if(!dv && !cv)v=false;
						if(v && !fixDogMap[pr[0]][pc[0]] && !fixDogMap[pr[1]][pc[1]]){
							max_s = sl[1][i];
							moves[2] = "L";
							moves[3] = str[i];
							mcv = cv;
							if(mrl[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							temp_re = new boolean[]{false,false};
							for(int n = 0; n < 2; n++){
								if(route_sum[n][2]>0){
									if(!fixMap[pr[n]][pc[n]-1] || (pc[n]-2>=0 && !fixMap[pr[n]][pc[n]-2])){
										temp_re[n] = true;
									}
								}
							}
						}
					}
				}
				cv = alertCheckDist(map,dogMap,cx[1],cy[1],0,1);
				if(cv){danger = 0;}else{danger = 1;}
				if(ar[1][i] && dog_r[1][i] && (sr[1][i]-danger>max_s || (ads[1]>0 && sr[1][i]-danger>max_s-1 && mr_check1 && !mrr[1][i]))){
					temp_moves[2] = "R";
					temp_moves[3] = str[i];
					fixMap = cal_rock_map(cx,cy,temp_moves,map);
					if(!fixMap[0][0]){
						pr[1] = cx[1] + sx[i];
						pc[1] = cy[1] + sy[i] +1;
						fixDogMap = dog_moves2(pr,pc,fixMap);
						boolean dv = alertCheckRock(map,dogMap,cx[1],cy[1],sx[i],sy[i]);
						boolean v = alertCheck(fixMap,fixDogMap,pr,pc);
						if(!dv && !cv)v=false;
						if(v && !fixDogMap[pr[0]][pc[0]] && !fixDogMap[pr[1]][pc[1]]){
							max_s = sr[1][i];
							moves[2] = "R";
							moves[3] = str[i];
							mcv = cv;
							if(mrr[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							temp_re = new boolean[]{false,false};
							for(int n = 0; n < 2; n++){
								if(route_sum[n][3]>0){
									if(!fixMap[pr[n]][pc[n]+1] || (pc[n]+2<=map_col-1 && !fixMap[pr[n]][pc[n]+2])){
										temp_re[n] = true;
									}
								}
							}
						}
					}
				}
			}
			if(x1<max_s)x1=max_s;
			s1 = max_s;
			if(moves[0].equals("N") && moves[2].equals("N"))break;
			int mr_cnt = 0;
			if(ads[0]>0 && mr_check0)mr_cnt++;
			if(ads[1]>0 && mr_check1)mr_cnt++;
			if(!mcv)mr_cnt++;
			boolean mm = true;
			if(s1>0 && s0==0)mm=false;
			if(s0>0 && s1==0)mm=false;
			if(pr[1]!=-1 && mm && (xscore<s0+s1-mr_cnt || (xscore==s0+s1-mr_cnt && xmr_cnt>mr_cnt))){
				xscore = s0 + s1;
				xmr_cnt = mr_cnt;
				xcv = mcv;
				xrock_exist[0] = temp_re[0];
				xrock_exist[1] = temp_re[1];
				route = moves[0] + moves[1] +"\n" + moves[2] + moves[3] + "\n";
			}
		}
		x_opti = xscore;
		rock_exist[0] = xrock_exist[0];
		rock_exist[1] = xrock_exist[1];
		if(route.equals(""))route = "N" + "\n" + "N" + "\n";
		return route;
	}
	boolean alertCheckRock(final boolean[][] rMap, final boolean[][] dMap, final int row, final int col,final int sx,final int sy){
		boolean v =true;
		int cnt = 0;
		if(sx==-1 && sy==0){
			if(row==1){
				cnt++;
			}else if(row==2){
				if(!rMap[row-1][col])
					cnt++;
			}else{
				if(!rMap[row-1][col] && (!rMap[row-2][col] || dMap[row-2][col]))
					cnt++;
			}
		}else if(sx==1 && sy==0){
			if(row==map_row-2){
				cnt++;
			}else if(row==map_row-3){
				if(!rMap[row+1][col])
					cnt++;
			}else{
				if(!rMap[row+1][col] && (!rMap[row+2][col] || dMap[row+2][col]))
					cnt++;
			}
		}else if(sx==0 && sy==-1){
			if(col==1){
				cnt++;
			}else if(col==2){
				if(!rMap[row][col-1])
					cnt++;
			}else{
				if(!rMap[row][col-1] && (!rMap[row][col-2] || dMap[row][col-2]))
					cnt++;
			}
		}else if(sx==0 && sy==1){	
			if(col==map_col-2){
				cnt++;
			}else if(col==map_col-3){
				if(!rMap[row][col+1])
					cnt++;
			}else{
				if(!rMap[row][col+1] &&  (!rMap[row][col+2] || dMap[row][col+2]))
					cnt++;
			}
		}
		
		if(cnt>0)
			v = false;
		return v;
	}

	boolean alertCheckDist(final boolean[][] rMap, final boolean[][] dMap, final int row, final int col,final int sx,final int sy){
		boolean v =true;
		int cnt = 0;
		if(sx==-1 && sy==0){
			if(row==1){
				cnt++;
			}else if(row==2){
				cnt++;
			}else{
				if(!rMap[row-1][col] || !rMap[row-2][col])
					cnt++;
			}
		}else if(sx==1 && sy==0){
			if(row==map_row-2){
				cnt++;
			}else if(row==map_row-3){
				cnt++;
			}else{
				if(!rMap[row+1][col] || !rMap[row+2][col])
					cnt++;
			}
		}else if(sx==0 && sy==-1){
			if(col==1){
				cnt++;
			}else if(col==2){
				cnt++;
			}else{
				if(!rMap[row][col-1] || !rMap[row][col-2])
					cnt++;
			}
		}else if(sx==0 && sy==1){	
			if(col==map_col-2){
				cnt++;
			}else if(col==map_col-3){
				cnt++;
			}else{
				if(!rMap[row][col+1] || !rMap[row][col+2])
					cnt++;
			}
		}
		
		boolean d = false;
		if(dMap[row-1][col])d = true;
		if(dMap[row+1][col])d = true;
		if(dMap[row][col-1])d = true;
		if(dMap[row][col+1])d = true;
		if(cnt>0 && d)
			v = false;
		return v;
	}

	boolean alertCheckSolo(final boolean[][] rMap, final boolean[][] dMap, final int row, final int col){
		boolean v = true;
		int cnt = 0;
		int alt = 0;
		if(row==1){
			cnt++;
		}else if(row==2){
			alt++;
			if(!rMap[row-1][col])
				cnt++;
		}else{
			if(!rMap[row-1][col]){
				if(!rMap[row-2][col]){
					cnt++;
				}else if(dMap[row-2][col]){
					cnt++;
				}else{
					alt++;
				}
			}
		}
		if(row==map_row-2){
			cnt++;
		}else if(row==map_row-3){
			alt++;
			if(!rMap[row+1][col])
				cnt++;
		}else{
			if(!rMap[row+1][col]){
				if(!rMap[row+2][col]){
					cnt++;
				}else if(dMap[row+2][col]){
					cnt++;
				}else{
					alt++;
				}
			}
		}
		if(col==1){
			cnt++;
		}else if(col==2){
			alt++;
			if(!rMap[row][col-1])
				cnt++;
		}else{
			if(!rMap[row][col-1]){
				if(!rMap[row][col-2]){
					cnt++;
				}else if(dMap[row][col-2]){
					cnt++;
				}else{
					alt++;
				}
			}
		}
		if(col==map_col-2){
			cnt++;
		}else if(col==map_col-3){
			alt++;
			if(!rMap[row][col+1])
				cnt++;
		}else{
			if(!rMap[row][col+1]){
				if(!rMap[row][col+2]){
					cnt++;
				}else if(dMap[row][col+2]){
					cnt++;
				}else{
					alt++;
				}
			}
		}
		boolean d = false;
		if(dMap[row-1][col])d = true;
		if(dMap[row+1][col])d = true;
		if(dMap[row][col-1])d = true;
		if(dMap[row][col+1])d = true;
		if(cnt+alt==3 && d)
			v = false;
		return v;
	}

	boolean alertCheck(final boolean[][] rMap, final boolean[][] dMap, final int[] row, final int[] col){
		boolean v = true;
		for(int n = 0; n < 2; n++){
			if(!v)break;
			int ni;
			if(n==0){ni=1;}else{ni=0;}
			int cnt = 0;
			int alt = 0;
			if(row[n]==1){
				cnt++;
			}else if(row[n]==2){
				alt++;
				if(!rMap[row[n]-1][col[n]])
					cnt++;
			}else{
				if(!rMap[row[n]-1][col[n]]){
					if(!rMap[row[n]-2][col[n]]){
						cnt++;
					}else if(dMap[row[n]-2][col[n]]){
						cnt++;
					}else if(row[n]-2==row[ni] && col[n]==col[ni]){
						cnt++;
					}else{
						alt++;
					}
				}
			}
			if(row[n]==map_row-2){
				cnt++;
			}else if(row[n]==map_row-3){
				alt++;
				if(!rMap[row[n]+1][col[n]])
					cnt++;
			}else{
				if(!rMap[row[n]+1][col[n]]){
					if(!rMap[row[n]+2][col[n]]){
						cnt++;
					}else if(dMap[row[n]+2][col[n]]){
						cnt++;
					}else if(row[n]+2==row[ni] && col[n]==col[ni]){
						cnt++;
					}else{
						alt++;
					}
				}
			}
			if(col[n]==1){
				cnt++;
			}else if(col[n]==2){
				alt++;
				if(!rMap[row[n]][col[n]-1])
					cnt++;
			}else{
				if(!rMap[row[n]][col[n]-1]){
					if(!rMap[row[n]][col[n]-2]){
						cnt++;
					}else if(dMap[row[n]][col[n]-2]){
						cnt++;
					}else if(row[n]==row[ni] && col[n]-2==col[ni]){
						cnt++;
					}else{
						alt++;
					}
				}
			}
			if(col[n]==map_col-2){
				cnt++;
			}else if(col[n]==map_col-3){
				alt++;
				if(!rMap[row[n]][col[n]+1])
					cnt++;
			}else{
				if(!rMap[row[n]][col[n]+1]){
					if(!rMap[row[n]][col[n]+2]){
						cnt++;
					}else if(dMap[row[n]][col[n]+2]){
						cnt++;
					}else if(row[n]==row[ni] && col[n]+2==col[ni]){
						cnt++;
					}else{
						alt++;
					}
				}
			}
			boolean d = false;
			if(dMap[row[n]-1][col[n]])d = true;
			if(dMap[row[n]+1][col[n]])d = true;
			if(dMap[row[n]][col[n]-1])d = true;
			if(dMap[row[n]][col[n]+1])d = true;
			if(cnt+alt>1 && d)
				v = false;
		}
		return v;
	}

	String decoyRoute(){
		String route ="";
		int[] sx = {-1,1,0,0,0};
		int[] sy = {0,0,-1,1,0};
		String[] str = {"U","D","L","R","N"};
		String[] moves = new String [4];
		int s0,s1;
		int max_s,xscore,xmr_cnt;
		int pr1 = cx[0],pc1 = cy[0],pr2 = cx[1],pc2 = cy[1];
		String[] opti_moves = new String [4];
		boolean mr_check0,mr_check1,alt_check,xalt_check;
		xscore = 0;
		xmr_cnt = 0;
		xalt_check = true;
		moves[0] = "N";
		moves[1] = "N";
		moves[2] = "N";
		moves[3] = "N";
		while(true){
			mr_check0 = false;
			mr_check1 = false;
			alt_check = false;
			flag_alert = true;
			max_s = 0;
			for(int i = 0; i < 5; i++){
				if(au[0][i]){
					if(dog_su[0][i]>max_s || (dog_su[0][i]==max_s && mr_check0 && !mru[0][i])){
						max_s = dog_su[0][i];
						moves[0] = "U";
						moves[1] = str[i];
						pr1 = cx[0] + sx[i] -1;
						pc1 = cy[0] + sy[i];
						if(mru[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(ad[0][i]){
					if(dog_sd[0][i]>max_s || (dog_sd[0][i]==max_s && mr_check0 && !mrd[0][i])){
						max_s = dog_sd[0][i];
						moves[0] = "D";
						moves[1] = str[i];
						pr1 = cx[0] + sx[i] +1;
						pc1 = cy[0] + sy[i];
						if(mrd[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(al[0][i]){
					if(dog_sl[0][i]>max_s || (dog_sl[0][i]==max_s && mr_check0 && !mrl[0][i])){
						max_s = dog_sl[0][i];
						moves[0] = "L";
						moves[1] = str[i];
						pr1 = cx[0] + sx[i];
						pc1 = cy[0] + sy[i] -1;
						if(mrl[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
				if(ar[0][i]){
					if(dog_sr[0][i]>max_s || (dog_sr[0][i]==max_s && mr_check0 && !mrr[0][i])){
						max_s = dog_sr[0][i];
						moves[0] = "R";
						moves[1] = str[i];
						pr1 = cx[0] + sx[i];
						pc1 = cy[0] + sy[i] +1;
						if(mrr[0][i]){
							mr_check0 = true;
						}else{
							mr_check0 = false;
						}
					}
				}
			}
			for(int i = 0; i < 5; i++){
				switch(moves[0]){
					case "U": 
					if(moves[1].equals(str[i])){
						dog_su[0][i] = 0;
					}
					break;
					case "D": 
					if(moves[1].equals(str[i])){
						dog_sd[0][i] = 0;
					}
					break;
					case "L": 
					if(moves[1].equals(str[i])){
						dog_sl[0][i] = 0;
					}
					break;
					case "R": 
					if(moves[1].equals(str[i])){
						dog_sr[0][i] = 0;
					}
					break;
				}
			}
			s0 = max_s;
			if(flag_done_serch && max_s==0)break;
			int[] decoy_pos = {-1,-1};
			int[] temp_decoy_pos = {-1,-1};
			String[] temp_moves = {moves[0],moves[1],"",""};
			max_s = 0;
			int ho = 0;
			int[] ads = new int[]{0,0};
			for(int i = 0; i < 2; i++){
				if(dogMap[cx[i]-1][cy[i]])ads[i]++;
				if(dogMap[cx[i]+1][cy[i]])ads[i]++;
				if(dogMap[cx[i]][cy[i]-1])ads[i]++;
				if(dogMap[cx[i]][cy[i]+1])ads[i]++;
			}
			for(int i = 0; i < 5; i++){
				if(au[1][i] && (dog_su[1][i]>max_s + ho || (ads[1]>0 && dog_su[1][i]>max_s-2 && mr_check1 && !mru[1][i]))){
					temp_moves[2] = "U";
					temp_moves[3] = str[i];
					if(cal_collision(temp_moves)){
						temp_decoy_pos = calcDecoyPos(temp_moves,pr1,pc1,cx[1] + sx[i] -1,cy[1] + sy[i]);
						if(temp_decoy_pos[0]!=-1){
							max_s = dog_su[1][i];
							moves[2] = "U";
							moves[3] = str[i];
							pr2 = cx[1] + sx[i] -1;
							pc2 = cy[1] + sy[i];
							decoy_pos[0] = temp_decoy_pos[0];
							decoy_pos[1] = temp_decoy_pos[1];
							if(mru[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							alt_check = flag_alert;
						}
					}
				}
				if(ad[1][i] && (dog_sd[1][i]>max_s + ho || (ads[1]>0 && dog_sd[1][i]>max_s-2 && mr_check1 && !mrd[1][i]))){
					temp_moves[2] = "D";
					temp_moves[3] = str[i];
					if(cal_collision(temp_moves)){
						temp_decoy_pos = calcDecoyPos(temp_moves,pr1,pc1,cx[1] + sx[i] +1,cy[1] + sy[i]);
						if(temp_decoy_pos[0]!=-1){
							max_s = dog_sd[1][i];
							moves[2] = "D";
							moves[3] = str[i];
							pr2 = cx[1] + sx[i] +1;
							pc2 = cy[1] + sy[i];
							decoy_pos[0] = temp_decoy_pos[0];
							decoy_pos[1] = temp_decoy_pos[1];
							if(mrd[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							alt_check = flag_alert;
						}
					}
				}
				if(al[1][i] && (dog_sl[1][i]>max_s + ho || (ads[1]>0 && dog_sl[1][i]>max_s-2 && mr_check1 && !mrl[1][i]))){
					temp_moves[2] = "L";
					temp_moves[3] = str[i];
					if(cal_collision(temp_moves)){
						temp_decoy_pos = calcDecoyPos(temp_moves,pr1,pc1,cx[1] + sx[i],cy[1] + sy[i] -1);
						if(temp_decoy_pos[0]!=-1){
							max_s = dog_sl[1][i];
							moves[2] = "L";
							moves[3] = str[i];
							pr2 = cx[1] + sx[i];
							pc2 = cy[1] + sy[i] -1;
							decoy_pos[0] = temp_decoy_pos[0];
							decoy_pos[1] = temp_decoy_pos[1];
							if(mrl[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							alt_check = flag_alert;
						}
					}
				}
				if(ar[1][i] && (dog_sr[1][i]>max_s + ho || (ads[1]>0 && dog_sr[1][i]>max_s-2 && mr_check1 && !mrr[1][i]))){
					temp_moves[2] = "R";
					temp_moves[3] = str[i];
					if(cal_collision(temp_moves)){
						temp_decoy_pos = calcDecoyPos(temp_moves,pr1,pc1,cx[1] + sx[i],cy[1] + sy[i] +1);
						if(temp_decoy_pos[0]!=-1){
							max_s = dog_sr[1][i];
							moves[2] = "R";
							moves[3] = str[i];
							pr2 = cx[1] + sx[i];
							pc2 = cy[1] + sy[i] +1;
							decoy_pos[0] = temp_decoy_pos[0];
							decoy_pos[1] = temp_decoy_pos[1];
							if(mrr[1][i]){
								mr_check1 = true;
							}else{
								mr_check1 = false;
							}
							alt_check = flag_alert;
						}
					}
				}
			}
			s1 = max_s;
			if(moves[0].equals("N") && moves[2].equals("N"))break;
			int mr_cnt = 0;
			if(ads[0]>0 && mr_check0)mr_cnt++;
			if(ads[1]>0 && mr_check1)mr_cnt++;
			int hosei = 0;
			boolean mm = true;
			if(s1>0 && s0==0)mm=false;
			if(s0>0 && s1==0)mm=false;
			flag_done_serch = true;
			//System.out.println(xscore+" ? "+(s0+s1)+" ("+s0+" "+s1+") "+" decoy_pos:"+decoy_pos[0]+" "+decoy_pos[1]+" moves;"+moves[0]+moves[1]+" "+moves[2]+moves[3]+" prpc;"+pr1+" "+pc1+" "+pr2+" "+pc2+" xmr;"+xmr_cnt+" mr;"+mr_cnt+" ho;"+hosei);
			if(decoy_pos[0]!=-1 && mm && (xscore<s0+s1+ hosei - mr_cnt*2 || (xscore==s0+s1+ hosei - mr_cnt*2 && xmr_cnt>mr_cnt))){
				xscore = s0 + s1 + hosei;
				xmr_cnt = mr_cnt;
				xalt_check = alt_check;
				route = "5 " + decoy_pos[0] + " " + decoy_pos[1] + "\n" + moves[0] + moves[1] +"\n" + moves[2] + moves[3] + "\n";
				//System.out.println("[reload] decoy_pos:"+decoy_pos[0]+" "+decoy_pos[1]+" moves;"+moves[0]+moves[1]+" "+moves[2]+moves[3]+" prpc;"+pr1+" "+pc1+" "+pr2+" "+pc2);
			}
		}
		//System.out.println(route);
		//System.out.println("end");
		x_decoy = xscore;
		return route;
	}

	int[] calcDecoyPos(final String[] moves, final int pr1, final int pc1, final int pr2, final int pc2){
		boolean[][] fixDogMap = new boolean [map_row][map_col];
		boolean[][] fixMap = new boolean [map_row][map_col];
		boolean[][] tempfixDogMap = new boolean [map_row][map_col];
		boolean[][] tempfixMap = new boolean [map_row][map_col];
		int decoy_posr,decoy_posc;
		int[] pos = {-1,-1};
		int[] pos_summon = {-1,-1};
		int[] sx = {-1,1,0,0,0};
		int[] sy = {0,0,-1,1,0};
		boolean done = false;

		if(!done){
			boolean here = false,tdone = false;
			int tpos0 = -1,tpos1 = -1;
			//four angle near purpose
			for(int j = 0; j < 4; j++){
				decoy_posr = pr1+sx[j];
				decoy_posc = pc1+sy[j];
				if(map[decoy_posr][decoy_posc]){
					tempfixMap = cal_rock_map(cx,cy,moves,map);
					tempfixDogMap = dog_moves(decoy_posr,decoy_posc,tempfixMap);
					if(!tempfixDogMap[pr1][pc1] && !tempfixDogMap[pr2][pc2] && tempfixMap[decoy_posr][decoy_posc]){
						if(tempfixDogMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
							fixMap = cal_rock_map(cx,cy,moves,map);
							fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						}else{
							tdone = true;
							tpos0 = decoy_posr;
							tpos1 = decoy_posc;
						}
					}
				}
				if(done)break;
				decoy_posr = pr2+sx[j];
				decoy_posc = pc2+sy[j];
				if(map[decoy_posr][decoy_posc]){
					tempfixMap = cal_rock_map(cx,cy,moves,map);
					tempfixDogMap = dog_moves(decoy_posr,decoy_posc,tempfixMap);
					if(!tempfixDogMap[pr1][pc1] && !tempfixDogMap[pr2][pc2] && tempfixMap[decoy_posr][decoy_posc]){
						if(tempfixDogMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
							fixMap = cal_rock_map(cx,cy,moves,map);
							fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						}else{
							tdone = true;
							tpos0 = decoy_posr;
							tpos1 = decoy_posc;
						}
					}
				}
				if(done)break;
			}
			if(!done && tdone){
				done=true;
				pos[0] = tpos0;
				pos[1] = tpos1;
				fixMap = cal_rock_map(cx,cy,moves,map);
				fixDogMap = dog_moves(pos[0],pos[1],fixMap);
			}
		}
		if(!done){
			//four corners
			for(int j = 0; j < 5; j++){
				if(map[1+sx[j]][1+sy[j]]){
					decoy_posr = 1+sx[j];
					decoy_posc = 1+sy[j];
					if(map[decoy_posr][decoy_posc]){
						fixMap = cal_rock_map(cx,cy,moves,map);
						fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
						}
					}
				}
				if(done)break;
				if(map[1+sx[j]][12+sy[j]]){
					decoy_posr = 1+sx[j];
					decoy_posc = 12+sy[j];
					if(map[decoy_posr][decoy_posc]){
						fixMap = cal_rock_map(cx,cy,moves,map);
						fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
						}
					}
				}
				if(done)break;
				if(map[15+sx[j]][1+sy[j]]){
					decoy_posr = 15+sx[j];
					decoy_posc = 1+sy[j];
					if(map[decoy_posr][decoy_posc]){
						fixMap = cal_rock_map(cx,cy,moves,map);
						fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
						}
					}
				}
				if(done)break;
				if(map[15+sx[j]][12+sy[j]]){
					decoy_posr = 15+sx[j];
					decoy_posc = 12+sy[j];
					if(map[decoy_posr][decoy_posc]){
						fixMap = cal_rock_map(cx,cy,moves,map);
						fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
						}
					}
				}
				if(done)break;
			}
		}

		if(!done){
			//summon spot
			pos_summon = calcSummonSpot(cx,cy);
			for(int j = 0; j < 5; j++){
				decoy_posr = pos_summon[0] + sx[j];
				decoy_posc = pos_summon[1] + sy[j];
				if(map[decoy_posr][decoy_posc]){
					fixMap = cal_rock_map(cx,cy,moves,map);
					fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
					if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
						done = true;
						pos[0] = decoy_posr;
						pos[1] = decoy_posc;
					}
				}
				if(done)break;
			}
		}

		if(!done){
			//around self
			for(int j = 0; j < 5; j++){
				if(done)break;
				for(int k = 0; k < 2; k++){
					if(done)break;
					decoy_posr = cx[k] + sx[j];
					decoy_posc = cy[k] + sy[j];
					if(map[decoy_posr][decoy_posc]){
						fixMap = cal_rock_map(cx,cy,moves,map);
						fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
						if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
							done = true;
							pos[0] = decoy_posr;
							pos[1] = decoy_posc;
						}
					}
				}
			}
		}
		if(!done){
		//random pos
			int cnt = 0;
     		Random rnd = new Random();
			int rnr,rnc;
			while(true){
				rnr = rnd.nextInt(15) +1;
				rnc = rnd.nextInt(12) +1;
				if(map[rnr][rnc]){
					cnt++;
					decoy_posr = rnr;
					decoy_posc = rnc;
					fixMap = cal_rock_map(cx,cy,moves,map);
					fixDogMap = dog_moves(decoy_posr,decoy_posc,fixMap);
					if(!fixDogMap[pr1][pc1] && !fixDogMap[pr2][pc2] && fixMap[decoy_posr][decoy_posc]){
						done = true;
						pos[0] = decoy_posr;
						pos[1] = decoy_posc;
					}
				}
				if(cnt==50 || done)break;
			}
		}
		if(pos[0]!=-1)
			flag_alert = alertCheck(fixMap,fixDogMap,new int[]{pr1,pr2},new int[]{pc1,pc2});
		return pos;
	}

	int[] calcSummonSpot(final int[] row, final int[] col){
		int dist[][] = new int [map_row][map_col];
		int qr[] = new int [map_row*map_col];
		int qc[] = new int [map_row*map_col];
		for(int r = 0; r < map_row; r++){
			for(int c = 0; c < map_col; c++){
				dist[r][c] = -1;
				if(!map[r][c])dist[r][c] = -2;
			}
		}
		dist[row[0]][col[0]] = 0;
		dist[row[1]][col[1]] = 0;
		qr[0] = row[0];
		qc[0] = col[0];
		qr[1] = row[1];
		qc[1] = col[1];
		int qi = 0, qe = 2;
		while (qi < qe) {
			int r = qr[qi], c = qc[qi];
			++qi;
			for (int i = 0; i < 4; ++i) {
				int nr = r + dx[i], nc = c + dy[i];
				if (dist[nr][nc] == -1) {
					dist[nr][nc] = dist[r][c] + 1;
					qr[qe] = nr;
					qc[qe] = nc;
					++qe;
				}
			}
		}
		int max = -1;
		int[] pos = {-1,-1};
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				if(max<dist[i][j]){
					max = dist[i][j];
					pos[0] = i;
					pos[1] = j;
				}
			}
		}
		return pos;
	}

	boolean[][] orderNext(final boolean[][] rMap,final boolean[][] dMap, final int row[], final int col[]) {
		boolean[][] route = new boolean[4][5];
		boolean[] u,d,l,r;

		u = new boolean[5];
		d = new boolean[5];
		l = new boolean[5];
		r = new boolean[5];
		Arrays.fill(u, true);
		Arrays.fill(d, true);
		Arrays.fill(l, true);
		Arrays.fill(r, true);

		//move abel check (rock and dogs)
		//U
		if(row[0]==1){
			Arrays.fill(u, false);//UU,UD,UL,UR,U
			l[0] = false;//LU
			r[0] = false;//RU
		}else if(row[0]==2){
			u[0] = false;//UU
			if(!rMap[row[0]-1][col[0]]){
				Arrays.fill(u, false);//UU,UD,UL,UR,U
			}else if(dMap[row[0]-1][col[0]]){
				//Arrays.fill(dog_u, false);//UU,UD,UL,UR,U
				if(u[1] && !calcHit(row,col,new String[]{"U","D"},rMap))
					u[1] = false;//UD
				if(col[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-1][col[0]-2] || dMap[row[0]-1][col[0]-2] || (row[1]==row[0]-1 && col[1]==col[0]-2)))
					u[2] = false;//UL
				if(col[0]>1 && u[2] && !calcHit(row,col,new String[]{"U","L"},rMap))
					u[2] = false;//UL
				if(col[0]<map_col-2 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-1][col[0]+2] || dMap[row[0]-1][col[0]+2] || (row[1]==row[0]-1 && col[1]==col[0]+2)))
					u[3] = false;//UR
				if(col[0]<map_col-2 && u[3] && !calcHit(row,col,new String[]{"U","R"},rMap))
					u[3] = false;//UR
				if(u[4] && !calcHit(row,col,new String[]{"U",""},rMap))
					u[4] = false;//U
			}else{//UD,UL,UR,U
				if(u[1] && !calcHit(row,col,new String[]{"U","D"},rMap))
					u[1] = false;//UD
				if(col[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-1][col[0]-2] || dMap[row[0]-1][col[0]-2] || (row[1]==row[0]-1 && col[1]==col[0]-2)))
					u[2] = false;//UL
				if(col[0]>1 && u[2] && !calcHit(row,col,new String[]{"U","L"},rMap))
					u[2] = false;//UL
				if(col[0]<map_col-2 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-1][col[0]+2] || dMap[row[0]-1][col[0]+2] || (row[1]==row[0]-1 && col[1]==col[0]+2)))
					u[3] = false;//UR
				if(col[0]<map_col-2 && u[3] && !calcHit(row,col,new String[]{"U","R"},rMap))
					u[3] = false;//UR
				if(u[4] && !calcHit(row,col,new String[]{"U",""},rMap))
					u[4] = false;//U
				//if(daMap[row[0]][col[0]])
				//	dog_u[1] = false;//UD
				//if(daMap[row[0]-1][col[0]-1])
				//	dog_u[2] = false;//UL
				//if(daMap[row[0]-1][col[0]+1])
				//	dog_u[3] = false;//UR
				//if(daMap[row[0]-1][col[0]])
				//	dog_u[4] = false;//U
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!rMap[row[0]-1][col[0]]){
				if(row[1]==row[0]-2 && col[1]==col[0])
					check_sum = false;
				if(!rMap[row[0]-2][col[0]])
					check_sum = false;
				if(dMap[row[0]-2][col[0]])
					check_sum =false;
			}
			if(dMap[row[0]-1][col[0]])check_d = false;

			if(!check_sum){
				Arrays.fill(u, false);//UU,UD,UL,UR,U
			}else if(!check_d){
				//Arrays.fill(dog_u, false);//UU,UD,UL,UR,U
				if(u[1] && !calcHit(row,col,new String[]{"U","D"},rMap))
					u[1] = false;//UD
				if(col[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-1][col[0]-2] || dMap[row[0]-1][col[0]-2] || (row[1]==row[0]-1 && col[1]==col[0]-2)))
					u[2] = false;//UL
				if(col[0]>1 && u[2] && !calcHit(row,col,new String[]{"U","L"},rMap))
					u[2] = false;//UL
				if(col[0]<map_col-2 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-1][col[0]+2] || dMap[row[0]-1][col[0]+2] || (row[1]==row[0]-1 && col[1]==col[0]+2)))
					u[3] = false;//UR
				if(col[0]<map_col-2 && u[3] && !calcHit(row,col,new String[]{"U","R"},rMap))
					u[3] = false;//UR
				if(u[4] && !calcHit(row,col,new String[]{"U",""},rMap))
					u[4] = false;//U
			}else{//UD,UL,UR,U
				if(u[1] && !calcHit(row,col,new String[]{"U","D"},rMap))
					u[1] = false;//UD
				if(col[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-1][col[0]-2] || dMap[row[0]-1][col[0]-2] || (row[1]==row[0]-1 && col[1]==col[0]-2)))
					u[2] = false;//UL
				if(col[0]>1 && u[2] && !calcHit(row,col,new String[]{"U","L"},rMap))
					u[2] = false;//UL
				if(col[0]<map_col-2 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-1][col[0]+2] || dMap[row[0]-1][col[0]+2] || (row[1]==row[0]-1 && col[1]==col[0]+2)))
					u[3] = false;//UR
				if(col[0]<map_col-2 && u[3] && !calcHit(row,col,new String[]{"U","R"},rMap))
					u[3] = false;//UR
				if(u[4] && !calcHit(row,col,new String[]{"U",""},rMap))
					u[4] = false;//U
				//if(daMap[row[0]][col[0]])
				//	dog_u[1] = false;//UD
				//if(daMap[row[0]-1][col[0]-1])
				//	dog_u[2] = false;//UL
				//if(daMap[row[0]-1][col[0]+1])
				//	dog_u[3] = false;//UR
				//if(daMap[row[0]-1][col[0]])
				//	dog_u[4] = false;//U
			}
			if((!rMap[row[0]-1][col[0]] || !rMap[row[0]-2][col[0]]) && (!rMap[row[0]-3][col[0]] || dMap[row[0]-3][col[0]] || (row[1]==row[0]-3 && col[1]==col[0])))//UU
				u[0] = false;
			if(u[0] && !calcHit(row,col,new String[]{"U","U"},rMap))
				u[0] = false;//UU
			//if(daMap[row[0]-2][col[0]])
			//	dog_u[0] = false;
		}
		//D
		if(row[0]==map_row-2){
			Arrays.fill(d, false);//DU,DD,DL,DR,D
			l[1] = false;//LD
			r[1] = false;//RD
		}else if(row[0]==map_row-3){
			d[1] = false;//DD
			if(!rMap[row[0]+1][col[0]]){
				Arrays.fill(d, false);//DU,DD,DL,DR,D
			}else if(dMap[row[0]+1][col[0]]){
				//Arrays.fill(dog_d, false);//DU,DD,DL,DR,D
				if(d[0] && !calcHit(row,col,new String[]{"D","U"},rMap))
					d[0] = false;//DU
				if(col[0]>1 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+1][col[0]-2] || dMap[row[0]+1][col[0]-2] || (row[1]==row[0]+1 && col[1]==col[0]-2)))
					d[2] = false;//DL
				if(col[0]>1 && d[2] && !calcHit(row,col,new String[]{"D","L"},rMap))
					d[2] = false;//DL
				if(col[0]<map_col-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+1][col[0]+2] || dMap[row[0]+1][col[0]+2] || (row[1]==row[0]+1 && col[1]==col[0]+2)))
					d[3] = false;//DR
				if(col[0]<map_col-2 && d[3] && !calcHit(row,col,new String[]{"D","R"},rMap))
					d[3] = false;//DR
				if(d[4] && !calcHit(row,col,new String[]{"D",""},rMap))
					d[4] = false;//D
			}else{//DU,DL,DR,D
				if(d[0] && !calcHit(row,col,new String[]{"D","U"},rMap))
					d[0] = false;//DU
				if(col[0]>1 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+1][col[0]-2] || dMap[row[0]+1][col[0]-2] || (row[1]==row[0]+1 && col[1]==col[0]-2)))
					d[2] = false;//DL
				if(col[0]>1 && d[2] && !calcHit(row,col,new String[]{"D","L"},rMap))
					d[2] = false;//DL
				if(col[0]<map_col-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+1][col[0]+2] || dMap[row[0]+1][col[0]+2] || (row[1]==row[0]+1 && col[1]==col[0]+2)))
					d[3] = false;//DR
				if(col[0]<map_col-2 && d[3] && !calcHit(row,col,new String[]{"D","R"},rMap))
					d[3] = false;//DR
				if(d[4] && !calcHit(row,col,new String[]{"D",""},rMap))
					d[4] = false;//D
				//if(daMap[row[0]][col[0]])
				//	dog_d[0] = false;//DU
				//if(daMap[row[0]+1][col[0]-1])
				//	dog_d[2] = false;//DL
				//if(daMap[row[0]+1][col[0]+1])
				//	dog_d[3] = false;//DR
				//if(daMap[row[0]+1][col[0]])
				//	dog_d[4] = false;//D
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!rMap[row[0]+1][col[0]]){
				if(row[1]==row[0]+2 && col[1]==col[0])
					check_sum = false;
				if(!rMap[row[0]+2][col[0]])
					check_sum = false;
				if(dMap[row[0]+2][col[0]])
					check_sum =false;
			}
			if(dMap[row[0]+1][col[0]])check_d = false;

			if(!check_sum){
				Arrays.fill(d, false);//UU,UD,UL,UR,U
			}else if(!check_d){
				//Arrays.fill(dog_d, false);//UU,UD,UL,UR,U
				if(d[0] && !calcHit(row,col,new String[]{"D","U"},rMap))
					d[0] = false;//DU
				if(col[0]>1 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+1][col[0]-2] || dMap[row[0]+1][col[0]-2] || (row[1]==row[0]+1 && col[1]==col[0]-2)))
					d[2] = false;//DL
				if(col[0]>1 && d[2] && !calcHit(row,col,new String[]{"D","L"},rMap))
					d[2] = false;//DL
				if(col[0]<map_col-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+1][col[0]+2] || dMap[row[0]+1][col[0]+2] || (row[1]==row[0]+1 && col[1]==col[0]+2)))
					d[3] = false;//DR
				if(col[0]<map_col-2 && d[3] && !calcHit(row,col,new String[]{"D","R"},rMap))
					d[3] = false;//DR
				if(d[4] && !calcHit(row,col,new String[]{"D",""},rMap))
					d[4] = false;//D
			}else{//DU,DL,DR,D
				if(d[0] && !calcHit(row,col,new String[]{"D","U"},rMap))
					d[0] = false;//DU
				if(col[0]>1 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+1][col[0]-2] || dMap[row[0]+1][col[0]-2] || (row[1]==row[0]+1 && col[1]==col[0]-2)))
					d[2] = false;//DL
				if(col[0]>1 && d[2] && !calcHit(row,col,new String[]{"D","L"},rMap))
					d[2] = false;//DL
				if(col[0]<map_col-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+1][col[0]+2] || dMap[row[0]+1][col[0]+2] || (row[1]==row[0]+1 && col[1]==col[0]+2)))
					d[3] = false;//DR
				if(col[0]<map_col-2 && d[3] && !calcHit(row,col,new String[]{"D","R"},rMap))
					d[3] = false;//DR
				if(d[4] && !calcHit(row,col,new String[]{"D",""},rMap))
					d[4] = false;//D
				//if(daMap[row[0]][col[0]])
				//	dog_d[0] = false;//DU
				//if(daMap[row[0]+1][col[0]-1])
				//	dog_d[2] = false;//DL
				//if(daMap[row[0]+1][col[0]+1])
				//	dog_d[3] = false;//DR
				//if(daMap[row[0]+1][col[0]])
				//	dog_d[4] = false;//D
			}
			if((!rMap[row[0]+1][col[0]] || !rMap[row[0]+2][col[0]]) && (!rMap[row[0]+3][col[0]] || dMap[row[0]+3][col[0]] || (row[1]==row[0]+3 && col[1]==col[0])))
				d[1] = false;//DD
			if(d[1] && !calcHit(row,col,new String[]{"D","D"},rMap))
				d[1] = false;//DD
			//if(daMap[row[0]+2][col[0]])
			//	dog_d[1] = false;//DD
		}

		//L
		if(col[0]==1){
			Arrays.fill(l, false);
			u[2] = false;
			d[2] = false;
		}else if(col[0]==2){
			l[2] = false;//LL
			if(!rMap[row[0]][col[0]-1]){
				Arrays.fill(l, false);
			}else if(dMap[row[0]][col[0]-1]){
				//Arrays.fill(dog_l, false);
				if(row[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-2][col[0]-1] || dMap[row[0]-2][col[0]-1] || (row[1]==row[0]-2 && col[1]==col[0]-1)))
					l[0] = false;//LU
				if(row[0]>1 && l[0] && !calcHit(row,col,new String[]{"L","U"},rMap))
					l[0] = false;//LU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+2][col[0]-1] || dMap[row[0]+2][col[0]-1] || (row[1]==row[0]+2 && col[1]==col[0]-1)))
					l[1] = false;//LD
				if(row[0]<map_row-2 && l[1] && !calcHit(row,col,new String[]{"L","D"},rMap))
					l[1] = false;//LD
				if(l[3] && !calcHit(row,col,new String[]{"L","R"},rMap))
					l[3] = false;//LR
				if(l[4] && !calcHit(row,col,new String[]{"L",""},rMap))
					l[4] = false;//L
			}else{//LU,LD,LR,L
				if(row[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-2][col[0]-1] || dMap[row[0]-2][col[0]-1] || (row[1]==row[0]-2 && col[1]==col[0]-1)))
					l[0] = false;//LU
				if(row[0]>1 && l[0] && !calcHit(row,col,new String[]{"L","U"},rMap))
					l[0] = false;//LU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+2][col[0]-1] || dMap[row[0]+2][col[0]-1] || (row[1]==row[0]+2 && col[1]==col[0]-1)))
					l[1] = false;//LD
				if(row[0]<map_row-2 && l[1] && !calcHit(row,col,new String[]{"L","D"},rMap))
					l[1] = false;//LD
				if(l[3] && !calcHit(row,col,new String[]{"L","R"},rMap))
					l[3] = false;//LR
				if(l[4] && !calcHit(row,col,new String[]{"L",""},rMap))
					l[4] = false;//L
				//if(daMap[row[0]-1][col[0]-1])
				//	dog_l[0] = false;//LU
				//if(daMap[row[0]+1][col[0]-1])
				//	dog_l[1] = false;//LD
				//if(daMap[row[0]][col[0]])
				//	dog_l[3] = false;//LR
				//if(daMap[row[0]][col[0]-1])
				//	dog_l[4] = false;//L
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!rMap[row[0]][col[0]-1]){
				if(row[1]==row[0] && col[1]==col[0]-2)
					check_sum = false;
				if(!rMap[row[0]][col[0]-2])
					check_sum = false;
				if(dMap[row[0]][col[0]-2])
					check_sum =false;
			}
			if(dMap[row[0]][col[0]-1])check_d = false;

			if(!check_sum){
				Arrays.fill(l, false);//LU,LD,LL,LR,L
			}else if(!check_d){
				//Arrays.fill(dog_l, false);//LU,LD,LL,LR,L
				if(row[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-2][col[0]-1] || dMap[row[0]-2][col[0]-1] || (row[1]==row[0]-2 && col[1]==col[0]-1)))
					l[0] = false;//LU
				if(row[0]>1 && l[0] && !calcHit(row,col,new String[]{"L","U"},rMap))
					l[0] = false;//LU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+2][col[0]-1] || dMap[row[0]+2][col[0]-1] || (row[1]==row[0]+2 && col[1]==col[0]-1)))
					l[1] = false;//LD
				if(row[0]<map_row-2 && l[1] && !calcHit(row,col,new String[]{"L","D"},rMap))
					l[1] = false;//LD
				if(l[3] && !calcHit(row,col,new String[]{"L","R"},rMap))
					l[3] = false;//LR
				if(l[4] && !calcHit(row,col,new String[]{"L",""},rMap))
					l[4] = false;//L
			}else{//LU,LD,LR,L
				if(row[0]>1 && !rMap[row[0]-1][col[0]-1] && (!rMap[row[0]-2][col[0]-1] || dMap[row[0]-2][col[0]-1] || (row[1]==row[0]-2 && col[1]==col[0]-1)))
					l[0] = false;//LU
				if(row[0]>1 && l[0] && !calcHit(row,col,new String[]{"L","U"},rMap))
					l[0] = false;//LU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]-1] && (!rMap[row[0]+2][col[0]-1] || dMap[row[0]+2][col[0]-1] || (row[1]==row[0]+2 && col[1]==col[0]-1)))
					l[1] = false;//LD
				if(row[0]<map_row-2 && l[1] && !calcHit(row,col,new String[]{"L","D"},rMap))
					l[1] = false;//LD
				if(l[3] && !calcHit(row,col,new String[]{"L","R"},rMap))
					l[3] = false;//LR
				if(l[4] && !calcHit(row,col,new String[]{"L",""},rMap))
					l[4] = false;//L
				//if(daMap[row[0]-1][col[0]-1])
				//	dog_l[0] = false;//LU
				//if(daMap[row[0]+1][col[0]-1])
				//	dog_l[1] = false;//LD
				//if(daMap[row[0]][col[0]])
				//	dog_l[3] = false;//LR
				//if(daMap[row[0]][col[0]-1])
				//	dog_l[4] = false;//L
			}
			if((!rMap[row[0]][col[0]-1] || !rMap[row[0]][col[0]-2]) && (!rMap[row[0]][col[0]-3] || dMap[row[0]][col[0]-3] || (row[1]==row[0] && col[1]==col[0]-3)))
				l[2] = false;//LL
			if(l[2] && !calcHit(row,col,new String[]{"L","L"},rMap))
				l[2] = false;//LL
			//if(daMap[row[0]][col[0]-2])
			//	dog_l[2] = false;//LL
		}

		//R
		if(col[0]==map_col-2){
			Arrays.fill(r, false);
			u[3] = false;
			d[3] = false;
		}else if(col[0]==map_col-3){
			r[3] = false;//RR
			if(!rMap[row[0]][col[0]+1]){
				Arrays.fill(r, false);
			}else if(dMap[row[0]][col[0]+1]){
				//Arrays.fill(dog_r, false);
				if(row[0]>1 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-2][col[0]+1] || dMap[row[0]-2][col[0]+1] || (row[1]==row[0]-2 && col[1]==col[0]+1)))
					r[0] = false;//RU
				if(row[0]>1 && r[0] && !calcHit(row,col,new String[]{"R","U"},rMap))
					r[0] = false;//RU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+2][col[0]+1] || dMap[row[0]+2][col[0]+1] || (row[1]==row[0]+2 && col[1]==col[0]+1)))
					r[1] = false;//RD
				if(row[0]<map_row-2 && r[1] && !calcHit(row,col,new String[]{"R","D"},rMap))
					r[1] = false;//RD
				if(r[2] && !calcHit(row,col,new String[]{"R","L"},rMap))
					r[2] = false;//RL
				if(r[4] && !calcHit(row,col,new String[]{"R",""},rMap))
					r[4] = false;//R
			}else{//RU,RD,RL,R
				if(row[0]>1 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-2][col[0]+1] || dMap[row[0]-2][col[0]+1] || (row[1]==row[0]-2 && col[1]==col[0]+1)))
					r[0] = false;//RU
				if(row[0]>1 && r[0] && !calcHit(row,col,new String[]{"R","U"},rMap))
					r[0] = false;//RU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+2][col[0]+1] || dMap[row[0]+2][col[0]+1] || (row[1]==row[0]+2 && col[1]==col[0]+1)))
					r[1] = false;//RD
				if(row[0]<map_row-2 && r[1] && !calcHit(row,col,new String[]{"R","D"},rMap))
					r[1] = false;//RD
				if(r[2] && !calcHit(row,col,new String[]{"R","L"},rMap))
					r[2] = false;//RL
				if(r[4] && !calcHit(row,col,new String[]{"R",""},rMap))
					r[4] = false;//R
				//if(daMap[row[0]-1][col[0]+1])
				//	dog_r[0] = false;//RU
				//if(daMap[row[0]+1][col[0]+1])
				//	dog_r[1] = false;//RD
				//if(daMap[row[0]][col[0]])
				//	dog_r[2] = false;//RL
				//if(daMap[row[0]][col[0]+1])
				//	dog_r[4] = false;//R
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!rMap[row[0]][col[0]+1]){
				if(row[1]==row[0] && col[1]==col[0]+2)
					check_sum = false;
				if(!rMap[row[0]][col[0]+2])
					check_sum = false;
				if(dMap[row[0]][col[0]+2])
					check_sum =false;
			}
			if(dMap[row[0]][col[0]+1])check_d = false;

			if(!check_sum){
				Arrays.fill(r, false);
			}else if(!check_d){
				//Arrays.fill(dog_r, false);
				if(row[0]>1 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-2][col[0]+1] || dMap[row[0]-2][col[0]+1] || (row[1]==row[0]-2 && col[1]==col[0]+1)))
					r[0] = false;//RU
				if(row[0]>1 && r[0] && !calcHit(row,col,new String[]{"R","U"},rMap))
					r[0] = false;//RU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+2][col[0]+1] || dMap[row[0]+2][col[0]+1] || (row[1]==row[0]+2 && col[1]==col[0]+1)))
					r[1] = false;//RD
				if(row[0]<map_row-2 && r[1] && !calcHit(row,col,new String[]{"R","D"},rMap))
					r[1] = false;//RD
				if(r[2] && !calcHit(row,col,new String[]{"R","L"},rMap))
					r[2] = false;//RL
				if(r[4] && !calcHit(row,col,new String[]{"R",""},rMap))
					r[4] = false;//R
			}else{//RU,RD,RL,R
				if(row[0]>1 && !rMap[row[0]-1][col[0]+1] && (!rMap[row[0]-2][col[0]+1] || dMap[row[0]-2][col[0]+1] || (row[1]==row[0]-2 && col[1]==col[0]+1)))
					r[0] = false;//RU
				if(row[0]>1 && r[0] && !calcHit(row,col,new String[]{"R","U"},rMap))
					r[0] = false;//RU
				if(row[0]<map_row-2 && !rMap[row[0]+1][col[0]+1] && (!rMap[row[0]+2][col[0]+1] || dMap[row[0]+2][col[0]+1] || (row[1]==row[0]+2 && col[1]==col[0]+1)))
					r[1] = false;//RD
				if(row[0]<map_row-2 && r[1] && !calcHit(row,col,new String[]{"R","D"},rMap))
					r[1] = false;//RD
				if(r[2] && !calcHit(row,col,new String[]{"R","L"},rMap))
					r[2] = false;//RL
				if(r[4] && !calcHit(row,col,new String[]{"R",""},rMap))
					r[4] = false;//R
				//if(daMap[row[0]-1][col[0]+1])
				//	dog_r[0] = false;//RU
				//if(daMap[row[0]+1][col[0]+1])
				//	dog_r[1] = false;//RD
				//if(daMap[row[0]][col[0]])
				//	dog_r[2] = false;//RL
				//if(daMap[row[0]][col[0]+1])
				//	dog_r[4] = false;//R
			}
			if((!rMap[row[0]][col[0]+1] || !rMap[row[0]][col[0]+2]) && (!rMap[row[0]][col[0]+3] || dMap[row[0]][col[0]+3] || (row[1]==row[0] && col[1]==col[0]+3)))
				r[3] = false;//RR
			if(r[3] && !calcHit(row,col,new String[]{"R","R"},rMap))
				r[3] = false;//RR
			//if(daMap[row[0]][col[0]+2])
			//	dog_r[3] = false;//RR
		}
		for(int i = 0; i < 5; i++){
			route[0][i] = u[i];
			route[1][i] = d[i];
			route[2][i] = l[i];
			route[3][i] = r[i];
		}
		return route;	
	}

	boolean calcHit(final int row[], final int col[], final String[] moves, final boolean[][] fMap){
		//row[0] = mine, row[1] = sub
		//move[0],move[1]
		boolean v = true;
		boolean[][] fixMap = new boolean [map_row][map_col];
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				fixMap[i][j] = fMap[i][j];
			}
		}
		int rs = row[0];
		int cs = col[0];
		
		for (int i = 0; i < 2; i++) {
			int n = i;
			switch(moves[i]){
				case "U" : 
					if(!fixMap[rs-1][cs]){
						if(rs-2>-1 && !fixMap[rs-2][cs]){
							//error move
							v = false;
						}
						fixMap[rs-1][cs] = true;
						fixMap[rs-2][cs] = false;
					}
					rs -= 1;
					break;
				case "D" : 
					if(!fixMap[rs+1][cs]){
						if(rs+2<map_row && !fixMap[rs+2][cs]){
							//error move
							v = false;
						}
						fixMap[rs+1][cs] = true;
						fixMap[rs+2][cs] = false;
					}
					rs += 1;
					break;
				case "L" : 
					if(!fixMap[rs][cs-1]){
						if(cs-2>-1 && !fixMap[rs][cs-2]){
							//error move
							v = false;
						}
						fixMap[rs][cs-1] = true;
						fixMap[rs][cs-2] = false;
					}
					cs -= 1;
					break;
				case "R" : 
					if(!fixMap[rs][cs+1]){
						if(cs+2<map_col && !fixMap[rs][cs+2]){
							//error move
							v = false;
						}
						fixMap[rs][cs+1] = true;
						fixMap[rs][cs+2] = false;
					}
					cs += 1;
					break;
				case "N" : break;
			}
			if(!fixMap[rs][cs] || !fixMap[row[1]][col[1]]){
				//error move
				v = false;
			}
		}
		return v;
	}

	boolean cal_collision(final String[] moves){
		boolean v = true;
		boolean[][] fixMap = new boolean [map_row][map_col];
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				fixMap[i][j] = map[i][j];
			}
		}
		int[] rs = new int [2];
		int[] cs = new int [2];
		for(int i = 0; i < 2; i++){
			rs[i] = cx[i];
			cs[i] = cy[i];
		}
		for (int i = 0; i < 4; i++) {
			int n = i/2;
			switch(moves[i]){
				case "U" : 
					if(!fixMap[rs[n]-1][cs[n]]){
						if(rs[n]-2>-1 && !fixMap[rs[n]-2][cs[n]]){
							//error move
							v = false;
						}
						fixMap[rs[n]-1][cs[n]] = true;
						fixMap[rs[n]-2][cs[n]] = false;
					}
					rs[n] -= 1;
					break;
				case "D" : 
					if(!fixMap[rs[n]+1][cs[n]]){
						if(rs[n]+2<map_row && !fixMap[rs[n]+2][cs[n]]){
							//error move
							v = false;
						}
						fixMap[rs[n]+1][cs[n]] = true;
						fixMap[rs[n]+2][cs[n]] = false;
					}
					rs[n] += 1;
					break;
				case "L" : 
					if(!fixMap[rs[n]][cs[n]-1]){
						if(cs[n]-2>-1 && !fixMap[rs[n]][cs[n]-2]){
							//error move
							v = false;
						}
						fixMap[rs[n]][cs[n]-1] = true;
						fixMap[rs[n]][cs[n]-2] = false;
					}
					cs[n] -= 1;
					break;
				case "R" : 
					if(!fixMap[rs[n]][cs[n]+1]){
						if(cs[n]+2<map_col && !fixMap[rs[n]][cs[n]+2]){
							//error move
							v = false;
						}
						fixMap[rs[n]][cs[n]+1] = true;
						fixMap[rs[n]][cs[n]+2] = false;
					}
					cs[n] += 1;
					break;
				case "N" : break;
			}
			if(!fixMap[rs[0]][cs[0]] || !fixMap[rs[1]][cs[1]]){
				//error move
				v = false;
			}
		}
		return v;
	}

	boolean[][] cal_rock_map(final int[] row, final int[] col, final String[] moves, final boolean[][] fMap){
		boolean[][] fixMap = new boolean [map_row][map_col];
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				fixMap[i][j] = fMap[i][j];
			}
		}
		int[] rs = new int [2];
		int[] cs = new int [2];
		for(int i = 0; i < 2; i++){
			rs[i] = row[i];
			cs[i] = col[i];
		}
		for (int i = 0; i < 4; i++) {
			int n = i/2;
			switch(moves[i]){
				case "U" : 
					if(!fixMap[rs[n]-1][cs[n]]){
						if(rs[n]-2>-1 && !fixMap[rs[n]-2][cs[n]]){
							//error move
							fixMap[0][0] = true;
						}
						fixMap[rs[n]-1][cs[n]] = true;
						fixMap[rs[n]-2][cs[n]] = false;
					}
					rs[n] -= 1;
					break;
				case "D" : 
					if(!fixMap[rs[n]+1][cs[n]]){
						if(rs[n]+2<map_row && !fixMap[rs[n]+2][cs[n]]){
							//error move
							fixMap[0][0] = true;
						}
						fixMap[rs[n]+1][cs[n]] = true;
						fixMap[rs[n]+2][cs[n]] = false;
					}
					rs[n] += 1;
					break;
				case "L" : 
					if(!fixMap[rs[n]][cs[n]-1]){
						if(cs[n]-2>-1 && !fixMap[rs[n]][cs[n]-2]){
							//error move
							fixMap[0][0] = true;
						}
						fixMap[rs[n]][cs[n]-1] = true;
						fixMap[rs[n]][cs[n]-2] = false;
					}
					cs[n] -= 1;
					break;
				case "R" : 
					if(!fixMap[rs[n]][cs[n]+1]){
						if(cs[n]+2<map_col && !fixMap[rs[n]][cs[n]+2]){
							//error move
							fixMap[0][0] = true;
						}
						fixMap[rs[n]][cs[n]+1] = true;
						fixMap[rs[n]][cs[n]+2] = false;
					}
					cs[n] += 1;
					break;
				case "N" : break;
			}
			if(!fixMap[rs[0]][cs[0]] || !fixMap[rs[1]][cs[1]]){
				//error move
				fixMap[0][0] = true;
			}
		}
		return fixMap;
	}

	int[][] cal_dist_one(final int row, final int col, final boolean[][] map_){
		int dist[][] = new int [map_row][map_col];
		int qr[] = new int [map_row*map_col];
		int qc[] = new int [map_row*map_col];
		for(int r = 0; r < map_row; r++){
			for(int c = 0; c < map_col; c++){
				dist[r][c] = Integer.MAX_VALUE;
			}
		}
		dist[row][col] = 0;
		qr[0] = row;
		qc[0] = col;
		int qi = 0, qe = 1;
		while (qi < qe) {
			int r = qr[qi], c = qc[qi];
			++qi;
			for (int i = 0; i < 4; ++i) {
				int nr = r + dx[i], nc = c + dy[i];
				if (map_[nr][nc] && dist[nr][nc] == Integer.MAX_VALUE) {
					dist[nr][nc] = dist[r][c] + 1;
					qr[qe] = nr;
					qc[qe] = nc;
					++qe;
				}
			}
		}
		return dist;
	}

	int initDistMap(){
		int leader = 0;
		int[][] tempDist = new int [map_row][map_col];
		int[][] pr,pc,cost;
		int[] lockr,lockc;
		int tr,tc,tcost;
		initDist = new int [2][map_row][map_col];
		initDist[0] = makeDistMap(cx[0],cy[0]);
		initDist[1] = makeDistMap(cx[1],cy[1]);
		pr = new int [2][8];
		pc = new int [2][8];
		cost = new int [2][8];
		lockr = new int [8];
		lockc = new int [8];

		for(int n = 0; n < 2; n++){
			Arrays.fill(pr[n],-1);
			Arrays.fill(pc[n],-1);
			Arrays.fill(cost[n],Integer.MAX_VALUE);
			int row = cx[n];
			int col = cy[n];
			for(int r = 1; r < map_row-1; r++){
				for(int c = 1; c < map_col-1; c++){
					int d = 0;
					for(int i = -1; i < 2; i++){
						for(int j = -1; j < 2; j++){
							if(dogMap[r+i][c+j])d++;
						}
					}
					if(d<2 && itemMap[r][c] && can_get_soul(r,c,map) && alertCheckSolo(map,dogMap,row,col)){
						for(int i = 0; i < 8; i++){
							if(cost[n][i]>initDist[n][r][c]){
								for(int j = 7 ; j > i; j--){
									cost[n][j] = cost[n][j-1];
									pr[n][j] = pr[n][j-1];
									pc[n][j] = pc[n][j-1];
								}
								cost[n][i] = initDist[n][r][c];
								pr[n][i] = r;
								pc[n][i] = c;
								break;
							}
						}
					}
				}
			}
		}
		boolean[] nns = new boolean[]{false,false};
		for(int n = 0; n < 2; n++){
			if(pr[n][0]==-1 || pc[n][0]==-1){
				nns[n] = true;
				continue;
			}
			tempDist = makeDistMap(pr[n][0],pc[n][0]);
			tcost = Integer.MAX_VALUE;
			tr = 0;
			tc = 0;
			int row = pr[n][0];
			int col = pc[n][0];
			for(int r = 1; r < map_row-1; r++){
				for(int c = 1; c < map_col-1; c++){
					if((r!=row || c!=col) && itemMap[r][c] && can_get_soul(r,c,map) && alertCheckSolo(map,dogMap,row,col)){
						if(tcost>tempDist[r][c] || (tcost==tempDist[r][c] && Math.abs(pr[n][0]-tr)+Math.abs(pc[n][0]-tc)<Math.abs(pr[n][0]-r)+Math.abs(pc[n][0]-c))){
							tcost = tempDist[r][c];
							tr = r;
							tc = c;
						}
					}
				}
			}
			cost[n][1] = tcost;
			pr[n][1] = tr;
			pc[n][1] = tc;
		}
		int sub;
		if(!(nns[0] && !nns[1]) && cost[0][0]+cost[0][1]<cost[1][0]+cost[1][1]){
			leader = 0;
			sub = 1;
		}else{
			leader = 1;
			sub = 0;
		}

		for(int i = 0; i < 2; i++){
			soul_r_next[i] = pr[leader][i];
			soul_c_next[i] = pc[leader][i];
			soul_r_next[i+2] = pr[sub][i+2];
			soul_c_next[i+2] = pc[sub][i+2];
			if(soul_r_next[i]<1)soul_r_next[i]=1;
			if(soul_c_next[i]<1)soul_c_next[i]=1;
			if(soul_r_next[i+2]<1)soul_r_next[i+2]=1;
			if(soul_c_next[i+2]<1)soul_c_next[i+2]=1;
		}
		return leader;
	}

	int[][] makeDistMap(final int row, final int col){
		int[][] dist = new int [map_row][map_col];
		int qr[] = new int [map_row*map_col];
		int qc[] = new int [map_row*map_col];
		dist = new int [map_row][map_col];
		for (int i = 0; i < dist.length; ++i)
			Arrays.fill(dist[i], Integer.MAX_VALUE);
		dist[row][col] = 0;
		qr[0] = row;
		qc[0] = col;
		int qi = 0, qe = 1;
		while (qi < qe) {
			int r = qr[qi], c = qc[qi];
			++qi;
			for (int i = 0; i < 4; ++i) {
				int nr = r + dx[i], nc = c + dy[i];
				int nr2 = nr + dx[i], nc2 = nc + dy[i];
				boolean v = true;
				if(v && (nr==0 || nc==0 || nr==map_row-1 || nc==map_col-1))
					v=false;
				if(v && !map[r][c] && !map[nr2][nc2] && dist[r][c]-1==dist[r-dx[i]][c-dy[i]]){
					int check_sum = 0;
					if(dist[r][c]-1==dist[r-1][c] && map[r+1][c])check_sum++;
					if(dist[r][c]-1==dist[r+1][c] && map[r-1][c])check_sum++;
					if(dist[r][c]-1==dist[r][c-1] && map[r][c+1])check_sum++;
					if(dist[r][c]-1==dist[r][c+1] && map[r][c-1])check_sum++;
					if(check_sum==1)
						v = false;
				}

				if(v && !map[nr][nc] && !map[nr2][nc2])
					v = false;
				if(v && dist[nr][nc] == Integer.MAX_VALUE){
					dist[nr][nc] = dist[r][c] + 1;
					qr[qe] = nr;
					qc[qe] = nc;
					++qe;
				}
			}
		}
		return dist;
	}


	int cal_soul_dist(final int num, final int row, final int col,final boolean[][] map_,final boolean[][] itemMap_){
		int[][] dist = new int [map_row][map_col];
		int qr[] = new int [map_row*map_col];
		int qc[] = new int [map_row*map_col];
		for (int i = 0; i < dist.length; ++i)
			//Arrays.fill(dist[i], Integer.MAX_VALUE);
			Arrays.fill(dist[i], 99);
		dist[row][col] = 0;

		qr[0] = row;
		qc[0] = col;
		int qi = 0, qe = 1;
		while (qi < qe) {
			int r = qr[qi], c = qc[qi];
			++qi;
			for (int i = 0; i < 4; ++i) {
				int nr = r + dx[i], nc = c + dy[i];
				int nr2 = nr + dx[i], nc2 = nc + dy[i];
				boolean v = true;
				if(v && (nr==0 || nc==0 || nr==map_row-1 || nc==map_col-1))v=false;
				if(v && !map_[r][c] && !map_[nr2][nc2] && dist[r][c]-1==dist[r-dx[i]][c-dy[i]]){
					int check_sum = 0;
					if(dist[r][c]-1==dist[r-1][c] && map[r+1][c])check_sum++;
					if(dist[r][c]-1==dist[r+1][c] && map[r-1][c])check_sum++;
					if(dist[r][c]-1==dist[r][c-1] && map[r][c+1])check_sum++;
					if(dist[r][c]-1==dist[r][c+1] && map[r][c-1])check_sum++;
					if(check_sum==1)
						v = false;
				}

				if(v && !map_[nr][nc] && !map_[nr2][nc2])v = false;
				if(v && dist[nr][nc] == 99){
					dist[nr][nc] = dist[r][c] + 1;
					qr[qe] = nr;
					qc[qe] = nc;
					++qe;
				}
			}
		}

		int min_dist = Integer.MAX_VALUE;
		int tr = 0, tc = 0;
		for(int r = 1; r < map_row-1; r++){
			//System.out.println();
			for(int c = 1; c < map_col-1; c++){
				int d = 0;
				for(int i = -1; i < 2; i++){
					for(int j = -1; j < 2; j++){
						if(dogMap[r+i][c+j])d++;
					}
				}
				if(d<2 && itemMap_[r][c] && can_get_soul(r,c,map_) && alertCheckSolo(map_,dogMap,row,col)){
					if(num==lead){
						int hosei = 0;
						if(min_dist>dist[r][c] + hosei){
							min_dist = dist[r][c] + hosei;
							tr = r;
							tc = c;
						}
					}else{
						int hosei = 8;
						for(int i = -1; i < 2; i++){
							for(int j = -1; j < 2; j++){
								if(itemMap_[r+i][c+j])hosei--;
								if(hosei<7)hosei--;
								if(hosei<0)hosei=0;
							}
						}
						//System.out.println("r "+r+" c "+c);
						if(min_dist>dist[r][c] + hosei){
							boolean v = true;
							if(soul_r_next[0]-3<r && r<soul_r_next[0]+3 && soul_c_next[0]-3<c && c<soul_c_next[0]+3)
								v = false;
							for(int i = 0; i < 2; i++){
								if(soul_r_next[i]==r && c==soul_c_next[i])
									v = false;
							}
							if(v && (!(cx[lead]<8 && cx[lead]-6<r && r<cx[lead]+5) || !(cx[lead]>7 && cx[lead]-5<r && r<cx[lead]+6)) && (!(cy[lead]>6 && cy[lead]-5<c && c<cy[lead]+6) || !(cy[lead]<7 && cy[lead]-6<c && c<cy[lead]+5))){
								//System.out.println("reload");
								min_dist = dist[r][c] + hosei;
								tr = r;
								tc = c;
							}
						}
					}
				}
			}
		}
		if(num==lead){
			int hosei = 4;
			int n;
			if(itemMap_[soul_r_next[0]][soul_c_next[0]]){	
				n = 0;		
			}else{
				n = 1;
			}
			if(min_dist==hosei + dist[soul_r_next[n]][soul_c_next[n]])
				min_dist--;
		}
		if(min_dist>47){
			for(int i = 0; i < map_row; i++){
				for(int j = 0; j < map_col; j++){
					if(dist[i][j]>98){
						dist[i][j] = Integer.MAX_VALUE;
					}else{
						dist[i][j] = 0;
					}
				}
			}
			int max = 1;
			for(int i = 0; i < map_row; i++){
				for(int j = 0; j < map_col; j++){
					if(dogMap[i][j]){
						for(int r = 0; r < map_row; r++){
							for(int c = 0; c < map_col; c++){
								if(dist[r][c]!=Integer.MAX_VALUE){
									dist[r][c] += 30-Math.abs(r-i)-Math.abs(c-j);
									if(max<dist[r][c])max=dist[r][c];
								}
							}
						}
					}
				}
			}
			min_dist = dist[row][col] * 25;
			min_dist /= max;
		}
		int res = 100 - min_dist*2;
		return res;
	}

	boolean can_get_soul(final int r,final int c, final boolean[][] imap){
		boolean v = true;
		if(!imap[r-1][c] && !imap[r+1][c] && !imap[r][c-1] && !imap[r][c+1]){
			if(!imap[r-1][c-1] && !imap[r+1][c+1])
				v = false;
			if(!imap[r-1][c+1] && !imap[r+1][c-1])
				v = false;
		}
		return v;
	}
	boolean near_soul_check(final int r,final int c){
		boolean v = true;
		if(c==cy[0] && r-2<cx[0] && cx[0]<r+2)v=false;
		if(r==cx[0] && c-2<cy[0] && cy[0]<c+2)v=false;
		for(int i = -1; i < 2; i++){
			for(int j = -1; j < 2; j++){
				if(i+cx[0]==r && j+cy[0]==c)v=false;
			}
		}
		return v;
	}

	boolean[][] dog_moves2(final int[] row, final int[] col, final boolean[][] map_){
		boolean[][] dog_curr = new boolean [map_row][map_col];
		boolean[][] dog_done = new boolean [map_row][map_col];
		int[][] dist = cal_dist(row,col,map_);
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				dog_curr[i][j] = dogMap[i][j];
			}
		}
		int cnt = 0;
		int hit;
		int min_id,min_r = -1,min_c = -1;
		boolean check;
		while(true){
			check = false;
			hit = 0;
			min_id = Integer.MAX_VALUE;
			for(int r = 0; r < map_row; r++){
				for(int c = 0; c < map_col; c++){
					if(dist[r][c]==cnt && !dog_done[r][c]){
						check = true;
						if(dog_curr[r][c]){
							hit++;
							if(min_id>dogIdMap[r][c]){
								min_id = dogIdMap[r][c];
								min_r = r;
								min_c = c;
							}
						}
					}
				}
			}
			if(min_id!=Integer.MAX_VALUE){
				dog_done[min_r][min_c] = true;
				if(dist[min_r-1][min_c]==cnt-1 && !dog_curr[min_r-1][min_c]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r-1][min_c] = true;
				}else if(dist[min_r][min_c-1]==cnt-1 && !dog_curr[min_r][min_c-1]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r][min_c-1] = true;
				}else if(dist[min_r][min_c+1]==cnt-1 && !dog_curr[min_r][min_c+1]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r][min_c+1] = true;
				}else if(dist[min_r+1][min_c]==cnt-1 && !dog_curr[min_r+1][min_c]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r+1][min_c] = true;
				}
			}
			if(hit<2)cnt++;
			if(!check)break;
		}
		return dog_curr;
	}

	int[][] cal_dist(final int[] row, final int[] col, boolean[][] fMap){
		int dist[][] = new int [map_row][map_col];
		int qr[] = new int [map_row*map_col];
		int qc[] = new int [map_row*map_col];
		for(int r = 0; r < map_row; r++){
			for(int c = 0; c < map_col; c++){
				dist[r][c] = Integer.MAX_VALUE;
			}
		}
		dist[row[0]][col[0]] = 0;
		dist[row[1]][col[1]] = 0;
		qr[0] = row[0];
		qc[0] = col[0];
		qr[1] = row[1];
		qc[1] = col[1];
		int qi = 0, qe = 2;
		while (qi < qe) {
			int r = qr[qi], c = qc[qi];
			++qi;
			for (int i = 0; i < 4; ++i) {
				int nr = r + dx[i], nc = c + dy[i];
				if (fMap[r][c] && dist[nr][nc] == Integer.MAX_VALUE) {
					dist[nr][nc] = dist[r][c] + 1;
					qr[qe] = nr;
					qc[qe] = nc;
					++qe;
				}
			}
		}
		return dist;
	}


	boolean[][] dog_moves(final int row, final int col, final boolean[][] map_){
		boolean[][] dog_curr = new boolean [map_row][map_col];
		boolean[][] dog_done = new boolean [map_row][map_col];
		int[][] dist = cal_dist_one(row,col,map_);
		for(int i = 0; i < map_row; i++){
			for(int j = 0; j < map_col; j++){
				dog_curr[i][j] = dogMap[i][j];
			}
		}
		int cnt = 0;
		int hit;
		int min_id,min_r = -1,min_c = -1;
		boolean check;
		while(true){
			check = false;
			hit = 0;
			min_id = Integer.MAX_VALUE;
			for(int r = 0; r < map_row; r++){
				for(int c = 0; c < map_col; c++){
					if(dist[r][c]==cnt && !dog_done[r][c]){
						check = true;
						if(dog_curr[r][c]){
							hit++;
							if(min_id>dogIdMap[r][c]){
								min_id = dogIdMap[r][c];
								min_r = r;
								min_c = c;
							}
						}
					}
				}
			}
			if(min_id!=Integer.MAX_VALUE){
				dog_done[min_r][min_c] = true;
				if(dist[min_r-1][min_c]==cnt-1 && !dog_curr[min_r-1][min_c]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r-1][min_c] = true;
				}else if(dist[min_r][min_c-1]==cnt-1 && !dog_curr[min_r][min_c-1]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r][min_c-1] = true;
				}else if(dist[min_r][min_c+1]==cnt-1 && !dog_curr[min_r][min_c+1]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r][min_c+1] = true;
				}else if(dist[min_r+1][min_c]==cnt-1 && !dog_curr[min_r+1][min_c]){
					dog_curr[min_r][min_c] = false;
					dog_curr[min_r+1][min_c] = true;
				}
			}
			if(hit<2)cnt++;
			if(!check)break;
		}
		return dog_curr;
	}


	void order(final int num, final int row, final int col) {
		//String route = "N";
		boolean n = true;
		int num_inv;
		if(num==0){num_inv=1;}else{num_inv=0;}
		for(int i = 0; i < 5; i++){
			au[num][i] = true;
			ad[num][i] = true;
			al[num][i] = true;
			ar[num][i] = true;

			dog_u[num][i] = true;
			dog_d[num][i] = true;
			dog_l[num][i] = true;
			dog_r[num][i] = true;
		}
		//move abel check (rock and dogs)
		//U
		if(row==1){
			Arrays.fill(au[num], false);//UU,UD,UL,UR,U
			al[num][0] = false;//LU
			ar[num][0] = false;//RU
		}else if(row==2){
			au[num][0] = false;//UU
			if(!map[row-1][col]){
				Arrays.fill(au[num], false);//UU,UD,UL,UR,U
			}else if(dogMap[row-1][col]){
				Arrays.fill(dog_u[num], false);//UU,UD,UL,UR,U
				//if(au[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","D"}))
				//	au[num][1] = false;//UD
				if(col>1 && !map[row-1][col-1] && (!map[row-1][col-2] || dogMap[row-1][col-2] || (cx[1]==row-1 && cy[1]==col-2)))
					au[num][2] = false;//UL
				//if(col>1 && au[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","L"}))
				//	au[num][2] = false;//UL
				if(col<map_col-2 && !map[row-1][col+1] && (!map[row-1][col+2] || dogMap[row-1][col+2] || (cx[1]==row-1 && cy[1]==col+2)))
					au[num][3] = false;//UR
				//if(col<map_col-2 && au[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","R"}))
				//	au[num][3] = false;//UR
				//if(au[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U",""}))
				//	au[num][4] = false;//U
			}else{//UD,UL,UR,U
				//if(au[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","D"}))
				//	au[num][1] = false;//UD
				if(col>1 && !map[row-1][col-1] && (!map[row-1][col-2] || dogMap[row-1][col-2] || (cx[1]==row-1 && cy[1]==col-2)))
					au[num][2] = false;//UL
				//if(col>1 && au[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","L"}))
				//	au[num][2] = false;//UL
				if(col<map_col-2 && !map[row-1][col+1] && (!map[row-1][col+2] || dogMap[row-1][col+2] || (cx[1]==row-1 && cy[1]==col+2)))
					au[num][3] = false;//UR
				//if(col<map_col-2 && au[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","R"}))
				//	au[num][3] = false;//UR
				//if(au[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U",""}))
				//	au[num][4] = false;//U
				if(dogAlertMap[row][col])
					dog_u[num][1] = false;//UD
				if(dogAlertMap[row-1][col-1])
					dog_u[num][2] = false;//UL
				if(dogAlertMap[row-1][col+1])
					dog_u[num][3] = false;//UR
				if(dogAlertMap[row-1][col])
					dog_u[num][4] = false;//U
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!map[row-1][col]){
				if(cx[num_inv]==row-2 && cy[num_inv]==col)
					check_sum = false;
				if(!map[row-2][col])
					check_sum = false;
				if(dogMap[row-2][col])
					check_sum =false;
			}
			if(dogMap[row-1][col])check_d = false;

			if(!check_sum){
				Arrays.fill(au[num], false);//UU,UD,UL,UR,U
			}else if(!check_d){
				Arrays.fill(dog_u[num], false);//UU,UD,UL,UR,U
				//if(au[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","D"}))
				//	au[num][1] = false;//UD
				if(col>1 && !map[row-1][col-1] && (!map[row-1][col-2] || dogMap[row-1][col-2] || (cx[1]==row-1 && cy[1]==col-2)))
					au[num][2] = false;//UL
				//if(col>1 && au[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","L"}))
				//	au[num][2] = false;//UL
				if(col<map_col-2 && !map[row-1][col+1] && (!map[row-1][col+2] || dogMap[row-1][col+2] || (cx[1]==row-1 && cy[1]==col+2)))
					au[num][3] = false;//UR
				//if(col<map_col-2 && au[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","R"}))
				//	au[num][3] = false;//UR
				//if(au[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U",""}))
				//	au[num][4] = false;//U
			}else{//UD,UL,UR,U
				//if(au[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","D"}))
				//	au[num][1] = false;//UD
				if(col>1 && !map[row-1][col-1] && (!map[row-1][col-2] || dogMap[row-1][col-2] || (cx[1]==row-1 && cy[1]==col-2)))
					au[num][2] = false;//UL
				//if(col>1 && au[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","L"}))
				//	au[num][2] = false;//UL
				if(col<map_col-2 && !map[row-1][col+1] && (!map[row-1][col+2] || dogMap[row-1][col+2] || (cx[1]==row-1 && cy[1]==col+2)))
					au[num][3] = false;//UR
				//if(col<map_col-2 && au[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","R"}))
				//	au[num][3] = false;//UR
				//if(au[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U",""}))
				//	au[num][4] = false;//U
				if(dogAlertMap[row][col])
					dog_u[num][1] = false;//UD
				if(dogAlertMap[row-1][col-1])
					dog_u[num][2] = false;//UL
				if(dogAlertMap[row-1][col+1])
					dog_u[num][3] = false;//UR
				if(dogAlertMap[row-1][col])
					dog_u[num][4] = false;//U
			}
			if((!map[row-1][col] || !map[row-2][col]) && (!map[row-3][col] || dogMap[row-3][col] || (cx[1]==row-3 && cy[1]==col)))//UU
				au[num][0] = false;
			//if(au[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"U","U"}))
			//	au[num][0] = false;//UU
			if(dogAlertMap[row-2][col])
				dog_u[num][0] = false;
		}
		//D
		if(row==map_row-2){
			Arrays.fill(ad[num], false);//DU,DD,DL,DR,D
			al[num][1] = false;//LD
			ar[num][1] = false;//RD
		}else if(row==map_row-3){
			ad[num][1] = false;//DD
			if(!map[row+1][col]){
				Arrays.fill(ad[num], false);//DU,DD,DL,DR,D
			}else if(dogMap[row+1][col]){
				Arrays.fill(dog_d[num], false);//DU,DD,DL,DR,D
				//if(ad[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","U"}))
				//	ad[num][0] = false;//DU
				if(col>1 && !map[row+1][col-1] && (!map[row+1][col-2] || dogMap[row+1][col-2] || (cx[1]==row+1 && cy[1]==col-2)))
					ad[num][2] = false;//DL
				//if(col>1 && ad[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","L"}))
				//	ad[num][2] = false;//DL
				if(col<map_col-2 && !map[row+1][col+1] && (!map[row+1][col+2] || dogMap[row+1][col+2] || (cx[1]==row+1 && cy[1]==col+2)))
					ad[num][3] = false;//DR
				//if(col<map_col-2 && ad[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","R"}))
				//	ad[num][3] = false;//DR
				//if(ad[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D",""}))
				//	ad[num][4] = false;//D
			}else{//DU,DL,DR,D
				//if(ad[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","U"}))
				//	ad[num][0] = false;//DU
				if(col>1 && !map[row+1][col-1] && (!map[row+1][col-2] || dogMap[row+1][col-2] || (cx[1]==row+1 && cy[1]==col-2)))
					ad[num][2] = false;//DL
				//if(col>1 && ad[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","L"}))
				//	ad[num][2] = false;//DL
				if(col<map_col-2 && !map[row+1][col+1] && (!map[row+1][col+2] || dogMap[row+1][col+2] || (cx[1]==row+1 && cy[1]==col+2)))
					ad[num][3] = false;//DR
				//if(col<map_col-2 && ad[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","R"}))
				//	ad[num][3] = false;//DR
				//if(ad[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D",""}))
				//	ad[num][4] = false;//D
				if(dogAlertMap[row][col])
					dog_d[num][0] = false;//DU
				if(dogAlertMap[row+1][col-1])
					dog_d[num][2] = false;//DL
				if(dogAlertMap[row+1][col+1])
					dog_d[num][3] = false;//DR
				if(dogAlertMap[row+1][col])
					dog_d[num][4] = false;//D
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!map[row+1][col]){
				if(cx[num_inv]==row+2 && cy[num_inv]==col)
					check_sum = false;
				if(!map[row+2][col])
					check_sum = false;
				if(dogMap[row+2][col])
					check_sum =false;
			}
			if(dogMap[row+1][col])check_d = false;

			if(!check_sum){
				Arrays.fill(ad[num], false);//UU,UD,UL,UR,U
			}else if(!check_d){
				Arrays.fill(dog_d[num], false);//UU,UD,UL,UR,U
				//if(ad[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","U"}))
				//	ad[num][0] = false;//DU
				if(col>1 && !map[row+1][col-1] && (!map[row+1][col-2] || dogMap[row+1][col-2] || (cx[1]==row+1 && cy[1]==col-2)))
					ad[num][2] = false;//DL
				//if(col>1 && ad[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","L"}))
				//	ad[num][2] = false;//DL
				if(col<map_col-2 && !map[row+1][col+1] && (!map[row+1][col+2] || dogMap[row+1][col+2] || (cx[1]==row+1 && cy[1]==col+2)))
					ad[num][3] = false;//DR
				//if(col<map_col-2 && ad[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","R"}))
				//	ad[num][3] = false;//DR
				//if(ad[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D",""}))
				//	ad[num][4] = false;//D
			}else{//DU,DL,DR,D
				//if(ad[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","U"}))
				//	ad[num][0] = false;//DU
				if(col>1 && !map[row+1][col-1] && (!map[row+1][col-2] || dogMap[row+1][col-2] || (cx[1]==row+1 && cy[1]==col-2)))
					ad[num][2] = false;//DL
				//if(col>1 && ad[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","L"}))
				//	ad[num][2] = false;//DL
				if(col<map_col-2 && !map[row+1][col+1] && (!map[row+1][col+2] || dogMap[row+1][col+2] || (cx[1]==row+1 && cy[1]==col+2)))
					ad[num][3] = false;//DR
				//if(col<map_col-2 && ad[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","R"}))
				//	ad[num][3] = false;//DR
				//if(ad[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D",""}))
				//	ad[num][4] = false;//D
				if(dogAlertMap[row][col])
					dog_d[num][0] = false;//DU
				if(dogAlertMap[row+1][col-1])
					dog_d[num][2] = false;//DL
				if(dogAlertMap[row+1][col+1])
					dog_d[num][3] = false;//DR
				if(dogAlertMap[row+1][col])
					dog_d[num][4] = false;//D
			}
			if((!map[row+1][col] || !map[row+2][col]) && (!map[row+3][col] || dogMap[row+3][col] || (cx[1]==row+3 && cy[1]==col)))
				ad[num][1] = false;//DD
			//if(ad[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"D","D"}))
			//	ad[num][1] = false;//DD
			if(dogAlertMap[row+2][col])
				dog_d[num][1] = false;//DD
		}

		//L
		if(col==1){
			Arrays.fill(al[num], false);
			au[num][2] = false;
			ad[num][2] = false;
		}else if(col==2){
			al[num][2] = false;//LL
			if(!map[row][col-1]){
				Arrays.fill(al[num], false);
			}else if(dogMap[row][col-1]){
				Arrays.fill(dog_l[num], false);
				if(row>1 && !map[row-1][col-1] && (!map[row-2][col-1] || dogMap[row-2][col-1] || (cx[1]==row-2 && cy[1]==col-1)))
					al[num][0] = false;//LU
				//if(row>1 && al[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","U"}))
				//	al[num][0] = false;//LU
				if(row<map_row-2 && !map[row+1][col-1] && (!map[row+2][col-1] || dogMap[row+2][col-1] || (cx[1]==row+2 && cy[1]==col-1)))
					al[num][1] = false;//LD
				//if(row<map_row-2 && al[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","D"}))
				//	al[num][1] = false;//LD
				//if(al[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","R"}))
				//	al[num][3] = false;//LR
				//if(al[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L",""}))
				//	al[num][4] = false;//L
			}else{//LU,LD,LR,L
				if(row>1 && !map[row-1][col-1] && (!map[row-2][col-1] || dogMap[row-2][col-1] || (cx[1]==row-2 && cy[1]==col-1)))
					al[num][0] = false;//LU
				//if(row>1 && al[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","U"}))
				//	al[num][0] = false;//LU
				if(row<map_row-2 && !map[row+1][col-1] && (!map[row+2][col-1] || dogMap[row+2][col-1] || (cx[1]==row+2 && cy[1]==col-1)))
					al[num][1] = false;//LD
				//if(row<map_row-2 && al[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","D"}))
				//	al[num][1] = false;//LD
				//if(al[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","R"}))
				//	al[num][3] = false;//LR
				//if(al[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L",""}))
				//	al[num][4] = false;//L
				if(dogAlertMap[row-1][col-1])
					dog_l[num][0] = false;//LU
				if(dogAlertMap[row+1][col-1])
					dog_l[num][1] = false;//LD
				if(dogAlertMap[row][col])
					dog_l[num][3] = false;//LR
				if(dogAlertMap[row][col-1])
					dog_l[num][4] = false;//L
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!map[row][col-1]){
				if(cx[num_inv]==row && cy[num_inv]==col-2)
					check_sum = false;
				if(!map[row][col-2])
					check_sum = false;
				if(dogMap[row][col-2])
					check_sum =false;
			}
			if(dogMap[row][col-1])check_d = false;

			if(!check_sum){
				Arrays.fill(al[num], false);//LU,LD,LL,LR,L
			}else if(!check_d){
				Arrays.fill(dog_l[num], false);//LU,LD,LL,LR,L
				if(row>1 && !map[row-1][col-1] && (!map[row-2][col-1] || dogMap[row-2][col-1] || (cx[1]==row-2 && cy[1]==col-1)))
					al[num][0] = false;//LU
				//if(row>1 && al[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","U"}))
				//	al[num][0] = false;//LU
				if(row<map_row-2 && !map[row+1][col-1] && (!map[row+2][col-1] || dogMap[row+2][col-1] || (cx[1]==row+2 && cy[1]==col-1)))
					al[num][1] = false;//LD
				//if(row<map_row-2 && al[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","D"}))
				//	al[num][1] = false;//LD
				//if(al[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","R"}))
				//	al[num][3] = false;//LR
				//if(al[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L",""}))
				//	al[num][4] = false;//L
			}else{//LU,LD,LR,L
				if(row>1 && !map[row-1][col-1] && (!map[row-2][col-1] || dogMap[row-2][col-1] || (cx[1]==row-2 && cy[1]==col-1)))
					al[num][0] = false;//LU
				//if(row>1 && al[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","U"}))
				//	al[num][0] = false;//LU
				if(row<map_row-2 && !map[row+1][col-1] && (!map[row+2][col-1] || dogMap[row+2][col-1] || (cx[1]==row+2 && cy[1]==col-1)))
					al[num][1] = false;//LD
				//if(row<map_row-2 && al[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","D"}))
				//	al[num][1] = false;//LD
				//if(al[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","R"}))
				//	al[num][3] = false;//LR
				//if(al[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L",""}))
				//	al[num][4] = false;//L
				if(dogAlertMap[row-1][col-1])
					dog_l[num][0] = false;//LU
				if(dogAlertMap[row+1][col-1])
					dog_l[num][1] = false;//LD
				if(dogAlertMap[row][col])
					dog_l[num][3] = false;//LR
				if(dogAlertMap[row][col-1])
					dog_l[num][4] = false;//L
			}
			if((!map[row][col-1] || !map[row][col-2]) && (!map[row][col-3] || dogMap[row][col-3] || (cx[1]==row && cy[1]==col-3)))
				al[num][2] = false;//LL
			//if(al[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"L","L"}))
			//	al[num][2] = false;//LL
			if(dogAlertMap[row][col-2])
				dog_l[num][2] = false;//LL
		}

		//R
		if(col==map_col-2){
			Arrays.fill(ar[num], false);
			au[num][3] = false;
			ad[num][3] = false;
		}else if(col==map_col-3){
			ar[num][3] = false;//RR
			if(!map[row][col+1]){
				Arrays.fill(ar[num], false);
			}else if(dogMap[row][col+1]){
				Arrays.fill(dog_r[num], false);
				if(row>1 && !map[row-1][col+1] && (!map[row-2][col+1] || dogMap[row-2][col+1] || (cx[1]==row-2 && cy[1]==col+1)))
					ar[num][0] = false;//RU
				//if(row>1 && ar[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","U"}))
				//	ar[num][0] = false;//RU
				if(row<map_row-2 && !map[row+1][col+1] && (!map[row+2][col+1] || dogMap[row+2][col+1] || (cx[1]==row+2 && cy[1]==col+1)))
					ar[num][1] = false;//RD
				//if(row<map_row-2 && ar[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","D"}))
				//	ar[num][1] = false;//RD
				//if(ar[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","L"}))
				//	ar[num][2] = false;//RL
				//if(ar[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R",""}))
				//	ar[num][4] = false;//R
			}else{//RU,RD,RL,R
				if(row>1 && !map[row-1][col+1] && (!map[row-2][col+1] || dogMap[row-2][col+1] || (cx[1]==row-2 && cy[1]==col+1)))
					ar[num][0] = false;//RU
				//if(row>1 && ar[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","U"}))
				//	ar[num][0] = false;//RU
				if(row<map_row-2 && !map[row+1][col+1] && (!map[row+2][col+1] || dogMap[row+2][col+1] || (cx[1]==row+2 && cy[1]==col+1)))
					ar[num][1] = false;//RD
				//if(row<map_row-2 && ar[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","D"}))
				//	ar[num][1] = false;//RD
				//if(ar[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","L"}))
				//	ar[num][2] = false;//RL
				//if(ar[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R",""}))
				//	ar[num][4] = false;//R
				if(dogAlertMap[row-1][col+1])
					dog_r[num][0] = false;//RU
				if(dogAlertMap[row+1][col+1])
					dog_r[num][1] = false;//RD
				if(dogAlertMap[row][col])
					dog_r[num][2] = false;//RL
				if(dogAlertMap[row][col+1])
					dog_r[num][4] = false;//R
			}
		}else{
			boolean check_sum = true;
			boolean check_d = true;
			if(!map[row][col+1]){
				if(cx[num_inv]==row && cy[num_inv]==col+2)
					check_sum = false;
				if(!map[row][col+2])
					check_sum = false;
				if(dogMap[row][col+2])
					check_sum =false;
			}
			if(dogMap[row][col+1])check_d = false;

			if(!check_sum){
				Arrays.fill(ar[num], false);
			}else if(!check_d){
				Arrays.fill(dog_r[num], false);
				if(row>1 && !map[row-1][col+1] && (!map[row-2][col+1] || dogMap[row-2][col+1] || (cx[1]==row-2 && cy[1]==col+1)))
					ar[num][0] = false;//RU
				//if(row>1 && ar[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","U"}))
				//	ar[num][0] = false;//RU
				if(row<map_row-2 && !map[row+1][col+1] && (!map[row+2][col+1] || dogMap[row+2][col+1] || (cx[1]==row+2 && cy[1]==col+1)))
					ar[num][1] = false;//RD
				//if(row<map_row-2 && ar[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","D"}))
				//	ar[num][1] = false;//RD
				//if(ar[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","L"}))
				//	ar[num][2] = false;//RL
				//if(ar[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R",""}))
				//	ar[num][4] = false;//R
			}else{//RU,RD,RL,R
				if(row>1 && !map[row-1][col+1] && (!map[row-2][col+1] || dogMap[row-2][col+1] || (cx[1]==row-2 && cy[1]==col+1)))
					ar[num][0] = false;//RU
				//if(row>1 && ar[num][0] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","U"}))
				//	ar[num][0] = false;//RU
				if(row<map_row-2 && !map[row+1][col+1] && (!map[row+2][col+1] || dogMap[row+2][col+1] || (cx[1]==row+2 && cy[1]==col+1)))
					ar[num][1] = false;//RD
				//if(row<map_row-2 && ar[num][1] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","D"}))
				//	ar[num][1] = false;//RD
				//if(ar[num][2] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","L"}))
				//	ar[num][2] = false;//RL
				//if(ar[num][4] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R",""}))
				//	ar[num][4] = false;//R
				if(dogAlertMap[row-1][col+1])
					dog_r[num][0] = false;//RU
				if(dogAlertMap[row+1][col+1])
					dog_r[num][1] = false;//RD
				if(dogAlertMap[row][col])
					dog_r[num][2] = false;//RL
				if(dogAlertMap[row][col+1])
					dog_r[num][4] = false;//R
			}
			if((!map[row][col+1] || !map[row][col+2]) && (!map[row][col+3] || dogMap[row][col+3] || (cx[1]==row && cy[1]==col+3)))
				ar[num][3] = false;//RR
			//if(ar[num][3] && num==1 && !cal_collision(new String[]{order_str0.substring(0,1),order_str0.substring(1,2),"R","R"}))
			//	ar[num][3] = false;//RR
			if(dogAlertMap[row][col+2])
				dog_r[num][3] = false;//RR
		}

		
		int[] xr = {-1,1,0,0,0};
		int[] xc = {0,0,-1,1,0};
		boolean[][] MapCurrent = new boolean[map_row][map_col];
		boolean[][] itemMapCurrent = new boolean[map_row][map_col];
		for(int i = 0; i < 5; i++){
			su[num][i] = 0;
			sd[num][i] = 0;
			sl[num][i] = 0;
			sr[num][i] = 0;

			dog_su[num][i] = 0;
			dog_sd[num][i] = 0;
			dog_sl[num][i] = 0;
			dog_sr[num][i] = 0;

			//U
			if(au[num][i]){
				for(int r = 0; r < map_row; r++){
					for(int c = 0; c < map_col; c++){
						MapCurrent[r][c] = map[r][c];
						itemMapCurrent[r][c] = itemMap[r][c];
					}
				}
				//if(itemMap[row-1][col]){
				if(itemMap[row-1][col] && !(num!=lead && soul_r_next[0]-3<row-1 && row-1< soul_r_next[0]+3 && soul_c_next[0]-3<col && col<soul_c_next[0]+3)){
					if(dog_u[num][i]){
						su[num][i]+=soul_value;
						dog_su[num][i]+=soul_value;
					}else{
						dog_su[num][i]+=soul_value;
					}
					itemMapCurrent[row-1][col] = false;
				}
				// move rock
				if(!map[row-1][col]){
					mru[num][i] = true;
					MapCurrent[row-1][col] = true;
					MapCurrent[row-2][col] = false;
				}else if(!map[row-2][col] && !dogMap[row-1][col] && !itemMapCurrent[row-1][col]){
					mru[num][i] = true;
				}
				if(i!=4){
					if(itemMap[row-1+xr[i]][col+xc[i]]){
						if(dog_u[num][i] && !(num!=lead && soul_r_next[0]-3<row-1+xr[i] && row-1+xr[i]< soul_r_next[0]+3 && soul_c_next[0]-3<col+xc[i] && col+xc[i]<soul_c_next[0]+3)){
							su[num][i]+=soul_value;
							dog_su[num][i]+=soul_value;
						}else{
							dog_su[num][i]+=soul_value;
						}
						itemMapCurrent[row-1+xr[i]][col+xc[i]] = false;
					}
					// move rock
					if(!map[row-1+xr[i]][col+xc[i]]){
						mru[num][i] = true;
						MapCurrent[row-1+xr[i]][col+xc[i]] = true;
						MapCurrent[row-1+xr[i]*2][col+xc[i]*2] = false;
					}else if(!map[row-1+xr[i]*2][col+xc[i]*2]){
					}
				}
				if(i==4){
					int k = cal_soul_dist(num,row-1,col,MapCurrent,itemMapCurrent);
					if(dog_u[num][i]){
						su[num][i]+=k;
						if(i==1)su[num][i]-=1;
						dog_su[num][i]+=k;
					}else{
						dog_su[num][i]+=k;
					}
				}else{
					int k = cal_soul_dist(num,row-1+xr[i],col+xc[i],MapCurrent,itemMapCurrent);
					int kd = k;
					if(su[num][i]<soul_value && cx[num]==row-1+xr[i] && cy[num]==col+xc[i])k=0;
					if(dog_su[num][i]<soul_value && cx[num]==row-1+xr[i] && cy[num]==col+xc[i])k=0;
					if(dog_u[num][i]){
						su[num][i]+=k;
						if(i==1)su[num][i]-=1;
						dog_su[num][i]+=kd;
					}else{
						dog_su[num][i]+=kd;
					}
				}
				if(su[num][i]!=0)route_sum[num][0]++;
			}
			//D
			if(ad[num][i]){
				for(int r = 0; r < map_row; r++){
					for(int c = 0; c < map_col; c++){
						MapCurrent[r][c] = map[r][c];
						itemMapCurrent[r][c] = itemMap[r][c];
					}
				}
				if(itemMap[row+1][col] && !(num!=lead && soul_r_next[0]-3<row+1 && row+1< soul_r_next[0]+3 && soul_c_next[0]-3<col && col<soul_c_next[0]+3)){
					if(dog_d[num][i]){
						sd[num][i]+=soul_value;
						dog_sd[num][i]+=soul_value;
					}else{
						dog_sd[num][i]+=soul_value;
					}
					itemMapCurrent[row+1][col] = false;
				}
				// move rock
				if(!map[row+1][col]){
					mrd[num][i] = true;
					MapCurrent[row+1][col] = true;
					MapCurrent[row+2][col] = false;
				}else if(!map[row+2][col] && !dogMap[row+1][col] && !itemMapCurrent[row+1][col]){
					mrd[num][i] = true;
				}
				if(i!=4){
					if(itemMap[row+1+xr[i]][col+xc[i]] && !(num!=lead && soul_r_next[0]-3<row+1+xr[i] && row+1+xr[i]< soul_r_next[0]+3 && soul_c_next[0]-3<col+xc[i] && col+xc[i]<soul_c_next[0]+3)){
						if(dog_d[num][i]){
							sd[num][i]+=soul_value;
							dog_sd[num][i]+=soul_value;
						}else{
							dog_sd[num][i]+=soul_value;
						}
						itemMapCurrent[row+1+xr[i]][col+xc[i]] = false;
					}
					// move rock
					if(!map[row+1+xr[i]][col+xc[i]]){
						mrd[num][i] = true;
						MapCurrent[row+1+xr[i]][col+xc[i]] = true;
						MapCurrent[row+1+xr[i]*2][col+xc[i]*2] = false;
					}else if(!map[row+1+xr[i]*2][col+xc[i]*2]){
						//mrd[num][i] = true;
					}
				}
				if(i==4){
					int k = cal_soul_dist(num,row+1,col,MapCurrent,itemMapCurrent);
					if(dog_d[num][i]){
						sd[num][i]+=k;
						if(i==0)sd[num][i]-=1;
						dog_sd[num][i]+=k;
					}else{
						dog_sd[num][i]+=k;
					}
				}else{
					int k = cal_soul_dist(num,row+1+xr[i],col+xc[i],MapCurrent,itemMapCurrent);
					int kd = k;
					if(sd[num][i]<soul_value && cx[num]==row+1+xr[i] && cy[num]==col+xc[i])k=0;
					if(dog_sd[num][i]<soul_value && cx[num]==row+1+xr[i] && cy[num]==col+xc[i])kd=0;
					if(dog_d[num][i]){
						sd[num][i]+=k;
						if(i==0)sd[num][i]-=1;
						dog_sd[num][i]+=kd;
					}else{
						dog_sd[num][i]+=kd;
					}
				}
				if(sd[num][i]!=0)route_sum[num][1]++;
			}
			//L
			if(al[num][i]){
				for(int r = 0; r < map_row; r++){
					for(int c = 0; c < map_col; c++){
						MapCurrent[r][c] = map[r][c];
						itemMapCurrent[r][c] = itemMap[r][c];
					}
				}
				if(itemMap[row][col-1] && !(num!=lead && soul_r_next[0]-3<row && row< soul_r_next[0]+3 && soul_c_next[0]-3<col-1 && col-1<soul_c_next[0]+3)){
					if(dog_l[num][i]){
						sl[num][i]+=soul_value;
						dog_sl[num][i]+=soul_value;
					}else{
						dog_sl[num][i]+=soul_value;
					}
					itemMapCurrent[row][col-1] = false;
				}
				// move rock
				if(!map[row][col-1]){
					mrl[num][i] = true;
					MapCurrent[row][col-1] = true;
					MapCurrent[row][col-2] = false;
				}else if(!map[row][col-2] && !dogMap[row][col-1] && !itemMapCurrent[row][col-1]){
					mrl[num][i] = true;
				}
				if(i!=4){
					if(itemMap[row+xr[i]][col-1+xc[i]] && !(num!=lead && soul_r_next[0]-3<row+xr[i] && row+xr[i]< soul_r_next[0]+3 && soul_c_next[0]-3<col-1+xc[i] && col-1+xc[i]<soul_c_next[0]+3)){
						if(dog_l[num][i]){
							sl[num][i]+=soul_value;
							dog_sl[num][i]+=soul_value;
						}else{
							dog_sl[num][i]+=soul_value;
						}
						itemMapCurrent[row+xr[i]][col-1+xc[i]] = false;
					}
					// move rock
					if(!map[row+xr[i]][col-1+xc[i]]){
						mrl[num][i] = true;
						MapCurrent[row+xr[i]][col-1+xc[i]] = true;
						MapCurrent[row+xr[i]*2][col-1+xc[i]*2] = false;
					}else if(!map[row+xr[i]*2][col-1+xc[i]*2]){
						//mrl[num][i] = true;
					}
				}
				if(i==4){
					int k = cal_soul_dist(num,row,col-1,MapCurrent,itemMapCurrent);
					if(dog_l[num][i]){
						sl[num][i]+=k;
						if(i==3)sl[num][i]-=1;
						dog_sl[num][i]+=k;
					}else{
						dog_sl[num][i]+=k;
					}
				}else{
					int k = cal_soul_dist(num,row+xr[i],col-1+xc[i],MapCurrent,itemMapCurrent);
					int kd = k;
					if(sl[num][i]<soul_value && cx[num]==row+xr[i] && cy[num]==col-1+xc[i])k=0;
					if(dog_sl[num][i]<soul_value && cx[num]==row+xr[i] && cy[num]==col-1+xc[i])kd=0;
					if(dog_l[num][i]){
						sl[num][i]+=k;
						if(i==3)sl[num][i]-=1;
						dog_sl[num][i]+=kd;
					}else{
						dog_sl[num][i]+=kd;
					}
				}
				if(sl[num][i]!=0)route_sum[num][2]++;
			}
			//R
			if(ar[num][i]){
				for(int r = 0; r < map_row; r++){
					for(int c = 0; c < map_col; c++){
						MapCurrent[r][c] = map[r][c];
						itemMapCurrent[r][c] = itemMap[r][c];
					}
				}
				//if(itemMap[row][col+1]){
				if(itemMap[row][col+1] && !(num!=lead && soul_r_next[0]-3<row && row< soul_r_next[0]+3 && soul_c_next[0]-3<col+1 && col+1<soul_c_next[0]+3)){
					if(dog_r[num][i]){
						sr[num][i]+=soul_value;
						dog_sr[num][i]+=soul_value;
					}else{
						dog_sr[num][i]+=soul_value;
					}
					itemMapCurrent[row][col+1] = false;
				}
				// move rock
				if(!map[row][col+1]){
					mrr[num][i] = true;
					MapCurrent[row][col+1] = true;
					MapCurrent[row][col+2] = false;
				}else if(!map[row][col+2] && !dogMap[row][col+1] && !itemMapCurrent[row][col+1]){
					mrr[num][i] = true;
				}
				if(i!=4){
					//if(itemMap[row+xr[i]][col+1+xc[i]]){
					if(itemMap[row+xr[i]][col+1+xc[i]] && !(num!=lead && soul_r_next[0]-3<row+xr[i] && row+xr[i]< soul_r_next[0]+3 && soul_c_next[0]-3<col+1+xc[i] && col+1+xc[i]<soul_c_next[0]+3)){
						if(dog_r[num][i]){
							sr[num][i]+=soul_value;
							dog_sr[num][i]+=soul_value;
						}else{
							dog_sr[num][i]+=soul_value;
						}
						itemMapCurrent[row+xr[i]][col+1+xc[i]] = false;
					}
					// move rock
					if(!map[row+xr[i]][col+1+xc[i]]){
						mrr[num][i] = true;
						MapCurrent[row+xr[i]][col+1+xc[i]] = true;
						MapCurrent[row+xr[i]*2][col+1+xc[i]*2] = false;
					}else if(!map[row+xr[i]*2][col+1+xc[i]*2]){
						//mrr[num][i] = true;
					}
				}
				if(i==4){
					int k = cal_soul_dist(num,row,col+1,MapCurrent,itemMapCurrent);
					if(dog_r[num][i]){
						sr[num][i]+=k;
						if(i==2)sr[num][i]-=1;
						dog_sr[num][i]+=k;
					}else{
						dog_sr[num][i]+=k;
					}
				}else{
					int k = cal_soul_dist(num,row+xr[i],col+1+xc[i],MapCurrent,itemMapCurrent);
					int kd = k;
					if(sr[num][i]<soul_value && cx[num]==row+xr[i] && cy[num]==col+1+xc[i])k=0;
					if(dog_sr[num][i]<soul_value && cx[num]==row+xr[i] && cy[num]==col+1+xc[i])kd=0;
					if(dog_r[num][i]){
						sr[num][i]+=k;
						if(i==2)sr[num][i]-=1;
						dog_sr[num][i]+=kd;
					}else{
						dog_sr[num][i]+=kd;
					}
				}
				if(sr[num][i]!=0)route_sum[num][3]++;
			}
		}
		if(true){
			int[] wall = new int[]{0,0,0,0};
			int da = 0;
			if(dogMap[row-1][col])da++;
			if(dogMap[row+1][col])da++;
			if(dogMap[row][col-1])da++;
			if(dogMap[row][col+1])da++;
			
			if(!map[row-1][col])wall[0]++;
			if(!map[row+1][col])wall[1]++;
			if(!map[row][col-1])wall[2]++;
			if(!map[row][col+1])wall[3]++;

			if(da>0)da=2;
			for(int k = 0; k < da; k++){
				for(int i = 0; i < 2; i++){
					if(su[i][0]>0)su[i][0]++;
					if(dog_su[i][0]>0)dog_su[i][0]++;
					if(sd[i][1]>0)sd[i][1]++;
					if(dog_sd[i][1]>0)dog_sd[i][1]++;
					if(sl[i][2]>0)sl[i][2]++;
					if(dog_sl[i][2]>0)dog_sl[i][2]++;
					if(sr[i][3]>0)sr[i][3]++;
					if(dog_sr[i][3]>0)dog_sr[i][3]++;

					if(da>0){
						if(wall[0]>0){
							su[i][0]-=2;
							su[i][4]-=2;
							dog_su[i][0]-=2;
							dog_su[i][4]-=2;
							if(su[i][0]<0)su[i][0]=0;
							if(su[i][4]<0)su[i][4]=0;
							if(dog_su[i][0]<0)dog_su[i][0]=0;
							if(dog_su[i][4]<0)dog_su[i][4]=0;
						}
						if(wall[1]>0){
							sd[i][1]-=2;
							sd[i][4]-=2;
							dog_sd[i][1]-=2;
							dog_sd[i][4]-=2;
							if(sd[i][1]<0)sd[i][1]=0;
							if(sd[i][4]<0)sd[i][4]=0;
							if(dog_sd[i][1]<0)dog_sd[i][1]=0;
							if(dog_sd[i][4]<0)dog_sd[i][4]=0;
						}
						if(wall[2]>0){
							sl[i][2]-=2;
							sl[i][4]-=2;
							dog_sl[i][2]-=2;
							dog_sl[i][4]-=2;
							if(sl[i][2]<0)sl[i][2]=0;
							if(sl[i][4]<0)sl[i][4]=0;
							if(dog_sl[i][2]<0)dog_sl[i][2]=0;
							if(dog_sl[i][4]<0)dog_sl[i][4]=0;
						}
						if(wall[3]>0){
							sr[i][3]-=2;
							sr[i][4]-=2;
							dog_sr[i][3]-=2;
							dog_sr[i][4]-=2;
							if(sr[i][3]<0)sr[i][3]=0;
							if(sr[i][4]<0)sr[i][4]=0;
							if(dog_sr[i][3]<0)dog_sr[i][3]=0;
							if(dog_sr[i][4]<0)dog_sr[i][4]=0;
						}
					}
				}
			}
		}
	}
			
}
