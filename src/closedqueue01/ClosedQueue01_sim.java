package closedqueue01;

import java.util.ArrayList;
import java.util.Random;

public class ClosedQueue01_sim {
	private double p[][], mu[], people[]; //推移確率行列(今回は重力モデルから)
	private int time, node_index[];
	Random rnd = new Random();
	private int k; //ノード数
	private int n; //系内の客数
	ArrayList<Double> eventtime[]; 
	ArrayList<String> event[];
	ArrayList<Integer> queuelength[];
	private double timerate[][];
	private double timerate2[][];
	private double timeratecap[]; //キャパを超えている時間割合
	private double correlation[][];
	ArrayList<Integer> timequeue[];
	private int capacity; //当面キャパは全ノードで一緒
	private double maxlengthtime[];
	
	public ClosedQueue01_sim(double[][] p, int time, int k, int n, double[] mu, int[] node_index, double[] people) {
		this.p = p;
		this.time = time;
		this.k = k;
		this.n = n;
		this.mu = mu;
		this.node_index = node_index;
		this.people = people;
		eventtime = new ArrayList[k];
		event = new ArrayList[k];
		queuelength = new ArrayList[k];
		timequeue = new ArrayList[k];
		for(int i = 0; i < eventtime.length; i++) eventtime[i] = new ArrayList<Double>();
		for(int i = 0; i < event.length; i++) event[i] = new ArrayList<String>();
		for(int i = 0; i < queuelength.length; i++) queuelength[i] = new ArrayList<Integer>();
		for(int i = 0; i < timequeue.length; i++) timequeue[i] = new ArrayList<Integer>();
		timerate = new double[k][n+1]; //0人の場合も入る
		timerate2 = new double[n+1][k+1]; //0人からn人のn+1, 0拠点からk拠点でのk+1拠点
		timeratecap = new double[k+1]; //timeratecap[i]キャパを超えたノード数がiの延べ時間、ノード数0-k(k+1個)
		correlation = new double[k][k];
		capacity = 10; //当面キャパシティは10
		maxlengthtime = new double[k];
	}
	
	public double[][] getSimulation() {
		double service[] = new double[k];
		double result[][] = new double[2][k];
		int queue[] = new int[k]; //各ノードのサービス中を含むキューの長さ
		double elapse = 0;
		
		/*
		//スタート時に2つのノードに分散
		for(int i = 0; i < this.n / 2; i++) {
			//ノード0用
			event[0].add("arrival");
			queuelength[0].add(queue[0]);
			eventtime[0].add(elapse); //(移動時間0)
			queue[0]++; //最初はノード0にn人いるとする
			//ノードk用
			event[k-1].add("arrival");
			queuelength[k-1].add(queue[0]);
			eventtime[k-1].add(elapse); //(移動時間0)
			queue[k-1]++; 
		}
		*/
		for(int i = 0; i < people.length; i++) {
			for(int j = 0; j < people[i]; j++) {
				event[i].add("arrival");
				queuelength[i].add(queue[0]);
				eventtime[i].add(elapse); //(移動時間0)
				queue[i]++; //最初はノード0にn人いるとする
			}
		}
		
		
		service[0] = this.getExponential(mu[0]); //先頭客のサービス時間設定
		service[k-1] = this.getExponential(mu[k-1]); //先頭客のサービス時間設定
		double total_queue[] = new double[k]; //各ノードの延べ系内人数
		double total_queuelength[] = new double[k]; //待ち人数
		
		while(elapse < time) {
			double mini_service = 100000; //最小のサービス時間
			int mini_index = -1; //最小のサービス時間をもつノード
			
			for(int i = 0; i < k; i++) { //待ち人数がいる中で最小のサービス時間を持つノードを算出
				if( queue[i] > 0) {
					if( mini_service > service[i]) {
						mini_service = service[i];
						mini_index = i;
					}
				}
			}
			
			for(int i = 0; i < k; i++) { //ノードiから退去
				total_queue[i] += queue[i] * mini_service;
				if( queue[i] > 0) service[i] -= mini_service;
				if( queue[i] > 0 ) total_queuelength[i] += ( queue[i] - 1 ) * mini_service;
				else if ( queue[i] == 0 ) total_queuelength[i] += queue[i] * mini_service;
				timerate[i][queue[i]] += mini_service;
			}
			//各ノードでの人数割合(同時滞在人数) 
			for(int i = 0; i < n+1; i++) { //0人からn人までのn+1
				int totalnumber = 0;
				for(int j = 0; j < queue.length; j++) { //0拠点からk拠点
					if(queue[j] == i) totalnumber ++;
				}
				timerate2[i][totalnumber] += mini_service;
			}
			//キャパを超えるノードの割合
			int totalnumber = 0; //ノード数
			for(int i = 0; i < k; i++) {
				if( queue[i] > capacity ) totalnumber ++;  
			}
			timeratecap[totalnumber] += mini_service;
			
			//イベント時間での待ち人数を登録
			for(int i = 0; i < k; i++) timequeue[i].add(queue[i]);
			
			event[mini_index].add("departure");
			queuelength[mini_index].add(queue[mini_index]);
			queue[mini_index] --;
			elapse += mini_service;
			eventtime[mini_index].add(elapse); //経過時間の登録はイベント後
			if( queue[mini_index] > 0) service[mini_index] = this.getExponential(mu[mini_index]); //退去後まだ待ち人数がある場合、サービス時間設定
			
			//退去客の行き先決定
			double rand = rnd.nextDouble();
			double sum_rand = 0;
			int destination_index = -1;
			for(int i = 0; i < p[0].length; i++) {
				sum_rand += p[mini_index][i];
				if( rand < sum_rand) {
					destination_index = i;
					break;
				}
			}
			if( destination_index == -1) destination_index = p[0].length -1;
			event[destination_index].add("arrival");
			queuelength[destination_index].add(queue[destination_index]);
			eventtime[destination_index].add(elapse); //(移動時間0)
			//推移先で待っている客がいなければサービス時間設定(即時サービス)
			if(queue[destination_index] == 0) service[destination_index] = this.getExponential(mu[destination_index]);
			queue[destination_index] ++;			
		}
		
		for(int i = 0; i < k; i++) {
			result[0][i] = total_queue[i] / time; //平均系内人数
			result[1][i] = total_queuelength[i] / time; //平均待ち人数
		}
		return result;
		
	}
	
	public double[][] getEvaluation() {
		int maxLength[] = new int[k];
		double result[][] = new double[3][k]; //平均系内時間、系内時間分散、最大待ち行列の長さ
		for(int k = 0; k < this.k; k++) {
			for(int i = 0; i < eventtime[k].size(); i++) {
				//System.out.println("Eventtime[" + k + "] : "+eventtime[k].get(i)+" Event : "+ event[k].get(i)+" Queuelength : "+queuelength[k].get(i));
				if( maxLength[k] < queuelength[k].get(i) ) {
					maxLength[k] = queuelength[k].get(i);
					maxlengthtime[k] = eventtime[k].get(i);
				}
			}
		}
		for(int i = 0; i < maxlengthtime.length; i++) maxlengthtime[i] /= time;
		//System.out.println("Simulation : (MaxLengthTime) = "+Arrays.toString(maxlengthtime));
		
		int arrival_number[] = new int[k];
		int departure_number[] = new int[k];
		int arrival_index[] = new int[k]; 
		int departure_index[] = new int[k];
		double systemtime[] = new double[k];
		double systemtime2[] = new double[k];
		
		for(int k = 0; k < this.k; k++) {
			for(int i = 0; i < eventtime[k].size(); i++) { //同じ客の到着と退去を探す
				if(event[k].get(i) == "arrival") {
					arrival_number[k]++;
					arrival_index[k] = i;
					for(int j = departure_index[k] + 1; j < eventtime[k].size(); j++) {
						if(event[k].get(j) == "departure") {
							departure_number[k]++;
						}
						if( arrival_number[k] == departure_number[k]) {
							departure_index[k] = j;
							systemtime[k] += eventtime[k].get(departure_index[k]) - eventtime[k].get(arrival_index[k]);
							systemtime2[k] += Math.pow(eventtime[k].get(departure_index[k]) - eventtime[k].get(arrival_index[k]),2);
							break;
						}
					}
				}
			}
		}
		
		for(int i = 0; i < k; i++) {
			result[0][i] = systemtime[i] / departure_number[i];
			result[1][i] = systemtime2[i] / departure_number[i] - Math.pow((systemtime[i] / departure_number[i]),2);
			result[2][i] = maxLength[i];
		}
		return result;
	}
	
	public double[][] getTimerate() { //自分のノードでの時間割合
		for(int k = 0; k < this.k; k++) {
			for(int i = 0; i< timerate[k].length; i++) timerate[k][i] /= time ;
			for(int i = 0; i< timerate[k].length; i++) timerate[k][i] *= 100 ;
		}
		return timerate;
	}
		
	public double[][] getTimerate2() { //全ノードでの時間割合
		for(int n = 0; n < this.n; n++) {
			for(int i = 0; i< timerate2[n].length; i++) timerate2[n][i] /= time ;
			for(int i = 0; i< timerate2[n].length; i++) timerate2[n][i] *= 100 ;
		}
		return timerate2;
	}
	
	public double[] getTimeratecap() {
		for(int i = 0; i < k; i++) timeratecap[i] /= time;
		for(int i = 0; i < k; i++) timeratecap[i] *= 100;
		return timeratecap;
	}

		//指数乱数発生
		public double getExponential(double param) {
			return - Math.log(1 - rnd.nextDouble()) / param;
		}
		
		public double[][] getCorrelation(){//相関係数行列の作成
			//ArrayListから配列へ変換
			double elapsequeue [][] = new double[timequeue.length][timequeue[0].size()];
			for(int i = 0; i < k; i++) {
				for(int j = 0; j < timequeue[0].size(); j++) {
					elapsequeue[i][j] = timequeue[i].get(j);
				}
			}
			//相関係数行列作成
			for(int i = 0; i < k; i++) {
				for(int j = 0; j < k; j++) {
					//correlation[i][j] = new PearsonsCorrelation().correlation(elapsequeue[i], elapsequeue[j]);
					correlation[i][j] = getCorrelationCoefficient(elapsequeue[i], elapsequeue[j]);
				}
			}
			return correlation;
		}

		private double getCorrelationCoefficient(double[] dx, double[] dy) {
			// TODO Auto-generated method stub
		    //相関係数を求めるために用意する一時的な変数
		    double XAve = 0; //観測値のx 成分の平均値
		    double YAve = 0; //観測値のy 成分の平均値
		    double XVari = 0; //x の分散
		    double YVari = 0; //y の分散
		    double XYVari = 0; //xy の共分散

		    XAve = getAverage(dx);
		    YAve = getAverage(dy);

		    XVari = getVariance(dx,dx,XAve,XAve);
		    YVari = getVariance(dy,dy,YAve,YAve);
		    XYVari = getVariance(dx,dy,XAve,YAve);

		    return XYVari / (Math.sqrt(XVari * YVari));
		}


		private double getVariance(double[] dx, double[] dy, double xAve, double yAve) {
			// TODO Auto-generated method stub
			double tempvalXY = 0.0;
			for(int i =0 ; i < dx.length; i++) {
				 tempvalXY += (dx[i] - xAve) * (dy[i] - yAve);
			}
			return tempvalXY;
		}

		private double getAverage(double[] dx) {
			// TODO Auto-generated method stub
			double sum = 0;
			for(int i =0 ; i < dx.length; i++) {
				sum += dx[i];
			}
			return sum/dx.length;
		}

		public ArrayList<Integer>[] getTimequeue() {
			return timequeue;
		}

		public double[] getMaxlengthtime() {
			return maxlengthtime;
		}
}
