package closedqueue01;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class ClosedQueue01_main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Nステップ数、K拠点数
		int N = 100, K = 17;
		int user_node[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};//0使用-1不使用
		for(int i = 0; i < user_node.length; i++) {
			if(user_node[i] == -1)K--;
		}
		double [][]d = new double[K][K];
		double p[] = new double[K];
		double mu[] = new double[K];
		int node_index[] = new int[K];
		
		ClosedQueue01_main cmain = new ClosedQueue01_main();
		cmain.getCSV2("csv/distance.csv", user_node.length, user_node.length+3, d, p, mu, node_index, user_node);
		
		//確認用
		//double d[][] = {{1000,5,8,10,15},{5,1000,3,5,3},{8,3,1000,2,4},{10,5,2,1000,3},{15,3,4,3,1000}};//行を入力
		//double p[] = {10,15,5,5,5};//拠点人口表
		//double mu[] = {1,2,2,1,2};//サービス率
		
		double alpha[] = new double[mu.length],alpha1[] = new double[mu.length];
		ClosedQueue01_lib clib = new ClosedQueue01_lib(1,1,0.5,d,p, mu, N, K);
		double f[][] = new double [p.length][p.length];
		f = clib.calcGravity(); //fは推移確率行列
		
		//トラフィック方程式を解く準備
		double ff[][] = new double[mu.length -1][mu.length -1];
		double bb[] = new double[mu.length -1];
		for(int i = 0; i < mu.length -1; i++){
			for(int j = 0; j < mu.length -1; j++){
				if( i == j ) {
					ff[i][j] = f[j + 1][i + 1] - 1; 
				}else {
					ff[i][j] = f[j + 1][i + 1];
				}
			}
		}
		for(int i = 0;i < mu.length -1; i++){
			bb[i] = -f[0][i+1];
		}
		
		//alphaを求める
		clib.setA(ff);
		clib.setB(bb);
		alpha = clib.calcGauss();
		
		//alphaの配列の大きさが-1になってしまうので、元の大きさのalpha1に入れ直す
		for(int i = 0 ; i < alpha1.length; i++){
			if( i == 0) alpha1[i] = 1;
			else alpha1[i] = alpha[i-1];
		}
		
		clib.setAlpha(alpha1);
		
		//平均値解析法で平均系内人数を求める
		clib.calcAverage();
		double[] L = clib.getL();
		double[] R = clib.getR();
		double[] lambda = clib.getLambda();
				
		//理論値
		System.out.println("重力モデルパラメタ" +Arrays.deepToString(d));
		System.out.println("利用ノードID" +Arrays.toString(node_index));
		System.out.println("人気度" +Arrays.toString(p));
		System.out.println("サービス率" +Arrays.toString(mu));
		System.out.println("推移確率行列" +Arrays.deepToString(f));
		System.out.println("トラフィック方程式解" +Arrays.toString(alpha1));
		System.out.println("理論値 : 平均系内人数 = " +Arrays.toString(L));
		System.out.println("理論値 : 平均系内時間 = " +Arrays.toString(R));
		System.out.println("理論値 : スループット = " +Arrays.toString(lambda));
				
	}

	//複数種類のデータを一度に取り込む場合
	public void getCSV2(String path, int row, int column, double[][] d, double[] p, double[] mu, int[] node_index, int[] user_node) {
	//CSVから取り込み
		try {
			File f = new File(path);
			BufferedReader br = new BufferedReader(new FileReader(f));
				 
			String[][] data = new String[row][column]; //今回は[K][K+3]
			String line = br.readLine();
			for (int i = 0; line != null; i++) {
				data[i] = line.split(",", 0);
				line = br.readLine();
			}
			br.close();

			int rowindex = 0, columnindex = 0;
			// CSVから読み込んだ配列の中身を処理
			for(int i = 0; i < row; i++) {
				if(user_node[i] != -1) {
					columnindex = 0;
					for(int j = 0; j < column; j++) {
						if( j < column -3 ) {
							if(user_node[j] != -1) {
								d[rowindex][columnindex++] = Double.parseDouble(data[i][j]);
							}
						}
						else if (j == column -3) p[rowindex] = Double.parseDouble(data[i][j]);
						else if (j == column -2) mu[rowindex] = Double.parseDouble(data[i][j]);
						else if (j == column -1) node_index[rowindex] = Integer.parseInt(data[i][j]);
					}
					rowindex++;
				}
			} 

		} catch (IOException e) {
			System.out.println(e);
		}
		//CSVから取り込みここまで	
	}

}
