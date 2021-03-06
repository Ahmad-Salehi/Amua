/**
 * Amua - An open source modeling framework.
 * Copyright (C) 2017 Zachary J. Ward
 *
 * This file is part of Amua. Amua is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Amua is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Amua.  If not, see <http://www.gnu.org/licenses/>.
 */

package markov;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.table.DefaultTableModel;

import base.AmuaModel;

public class MarkovTraceSummary{
	public String traceName;
	int numStates;
	String dimSymbols[];
	public String dimNames[];
	public int numDim;
	public String stateNames[];
	//trace summaries
	public boolean discounted;
	public int maxCyclesGlobal;
	int maxCyclesLocal[];
	public double expectedValues[][], expectedValuesDis[][]; //[dim][mean/lb/ub]
	public double prev[][][]; //[state][mean/lb/ub][cycle]
	public double cycleRewards[][][], cycleRewardsDis[][][]; //[dim][mean/lb/ub][cycle]
	public double cumRewards[][][], cumRewardsDis[][][];
	public DefaultTableModel modelTraceRounded;
	DefaultTableModel modelTraceRaw;
	AmuaModel myModel;
	
	//Constructor
	public MarkovTraceSummary(MarkovTrace traces[]){
		//get names
		int numTraces=traces.length;
		traceName=traces[0].traceName;
		myModel=traces[0].myModel;
		numStates=traces[0].numStates;
		stateNames=traces[0].stateNames;
		numDim=traces[0].numDim;
		dimSymbols=traces[0].dimSymbols;
		dimNames=traces[0].dimNames;
		discounted=traces[0].discounted;
		//Build Model headers
		modelTraceRaw=new DefaultTableModel(); modelTraceRounded=new DefaultTableModel();
		modelTraceRaw.addColumn("Cycle"); modelTraceRounded.addColumn("Cycle");
		for(int s=0; s<numStates; s++){
			modelTraceRaw.addColumn(stateNames[s]+"_Mean"); modelTraceRaw.addColumn(stateNames[s]+"_LB"); modelTraceRaw.addColumn(stateNames[s]+"_UB");
			modelTraceRounded.addColumn(stateNames[s]+"_Mean"); modelTraceRounded.addColumn(stateNames[s]+"_LB"); modelTraceRounded.addColumn(stateNames[s]+"_UB");
		}
		for(int d=0; d<numDim; d++){
			modelTraceRaw.addColumn("Cycle_Mean_"+dimSymbols[d]); modelTraceRaw.addColumn("Cycle_LB_"+dimSymbols[d]); modelTraceRaw.addColumn("Cycle_UB_"+dimSymbols[d]);
			modelTraceRounded.addColumn("Cycle_Mean_"+dimSymbols[d]); modelTraceRounded.addColumn("Cycle_LB_"+dimSymbols[d]); modelTraceRounded.addColumn("Cycle_UB_"+dimSymbols[d]);
		}
		for(int d=0; d<numDim; d++){
			modelTraceRaw.addColumn("Cum_Mean_"+dimSymbols[d]); modelTraceRaw.addColumn("Cum_LB_"+dimSymbols[d]); modelTraceRaw.addColumn("Cum_UB_"+dimSymbols[d]);
			modelTraceRounded.addColumn("Cum_Mean_"+dimSymbols[d]); modelTraceRounded.addColumn("Cum_LB_"+dimSymbols[d]); modelTraceRounded.addColumn("Cum_UB_"+dimSymbols[d]);
		}
		//discounted
		if(discounted){
			for(int d=0; d<numDim; d++){
				modelTraceRaw.addColumn("Cycle_Dis_Mean_"+dimSymbols[d]); modelTraceRaw.addColumn("Cycle_Dis_LB_"+dimSymbols[d]); modelTraceRaw.addColumn("Cycle_Dis_UB_"+dimSymbols[d]);
				modelTraceRounded.addColumn("Cycle_Dis_Mean_"+dimSymbols[d]); modelTraceRounded.addColumn("Cycle_Dis_LB_"+dimSymbols[d]); modelTraceRounded.addColumn("Cycle_Dis_UB_"+dimSymbols[d]);
			}
			for(int d=0; d<numDim; d++){
				modelTraceRaw.addColumn("Cum_Dis_Mean_"+dimSymbols[d]); modelTraceRaw.addColumn("Cum_Dis_LB_"+dimSymbols[d]); modelTraceRaw.addColumn("Cum_Dis_UB_"+dimSymbols[d]);
				modelTraceRounded.addColumn("Cum_Dis_Mean_"+dimSymbols[d]); modelTraceRounded.addColumn("Cum_Dis_LB_"+dimSymbols[d]); modelTraceRounded.addColumn("Cum_Dis_UB_"+dimSymbols[d]);
			}
		}
		modelTraceRaw.addColumn("Num_Sims"); modelTraceRounded.addColumn("Num_Sims");
		
		//get max cycles
		maxCyclesLocal=new int[numTraces];
		maxCyclesLocal[0]=traces[0].cycles.size(); maxCyclesGlobal=maxCyclesLocal[0]; 
		for(int i=1; i<numTraces; i++){
			maxCyclesLocal[i]=traces[i].cycles.size(); maxCyclesGlobal=Math.max(maxCyclesGlobal, maxCyclesLocal[i]);
		}
		int numSims[]=new int[maxCyclesGlobal];
		for(int i=0; i<numTraces; i++){
			for(int j=0; j<maxCyclesLocal[i]; j++){
				numSims[j]++;
			}
		}
		
		//get final cum expected values
		expectedValues=new double[numDim][3]; expectedValuesDis=new double[numDim][3];
		for(int d=0; d<numDim; d++){
			double mean=0, meanDis=0;
			double curVal[]=new double[numTraces], curValDis[]=new double[numTraces];
			for(int t=0; t<numTraces; t++){
				int c=maxCyclesLocal[t]-1; //max cycle observed
				double val=traces[t].cumRewards[d].get(c);
				mean+=val; curVal[t]=val;
				if(discounted){
					val=traces[t].cumRewardsDis[d].get(c);
					meanDis+=val; curValDis[t]=val;
				}
			}
			expectedValues[d][0]=mean/(numTraces*1.0);
			Arrays.sort(curVal);
			int bounds[]=getBoundIndices(numTraces); int lb=bounds[0], ub=bounds[1];
			expectedValues[d][1]=curVal[lb]; expectedValues[d][2]=curVal[ub];
			if(discounted){
				expectedValuesDis[d][0]=meanDis/(numTraces*1.0);
				Arrays.sort(curValDis);
				expectedValuesDis[d][1]=curValDis[lb]; expectedValuesDis[d][2]=curValDis[ub];
			}
		}
		
		
		//initialize trace summaries
		prev=new double[numStates][3][maxCyclesGlobal];
		cycleRewards=new double[numDim][3][maxCyclesGlobal];
		cumRewards=new double[numDim][3][maxCyclesGlobal];
		if(discounted){
			cycleRewardsDis=new double[numDim][3][maxCyclesGlobal];
			cumRewardsDis=new double[numDim][3][maxCyclesGlobal];
		}
		
		//calculate prev
		for(int s=0; s<numStates; s++){
			for(int c=0; c<maxCyclesGlobal; c++){
				ArrayList<Double> curPrev=new ArrayList<Double>();
				double mean=0, denom=0;
				for(int t=0; t<numTraces; t++){
					if(maxCyclesLocal[t]>c){ //cycle observed
						double val=traces[t].prev[s].get(c);
						curPrev.add(val);
						mean+=val; denom++;
					}
				}
				prev[s][0][c]=mean/denom;
				Collections.sort(curPrev);
				int bounds[]=getBoundIndices(denom); int lb=bounds[0], ub=bounds[1];
				prev[s][1][c]=curPrev.get(lb); prev[s][2][c]=curPrev.get(ub);
			}
		}
		//calculate rewards
		for(int d=0; d<numDim; d++){
			for(int c=0; c<maxCyclesGlobal; c++){
				ArrayList<Double> curRewards= new ArrayList<Double>();
				ArrayList<Double> curCum= new ArrayList<Double>();
				double mean=0, meanCum=0, denom=0;
				for(int t=0; t<numTraces; t++){
					if(maxCyclesLocal[t]>c){ //cycle observed
						double val=traces[t].cycleRewards[d].get(c), valCum=traces[t].cumRewards[d].get(c);
						curRewards.add(val); curCum.add(valCum);
						mean+=val; meanCum+=valCum; denom++;
					}
				}
				cycleRewards[d][0][c]=mean/denom; cumRewards[d][0][c]=meanCum/denom;
				Collections.sort(curRewards); Collections.sort(curCum);
				int bounds[]=getBoundIndices(denom); int lb=bounds[0], ub=bounds[1];
				cycleRewards[d][1][c]=curRewards.get(lb); cycleRewards[d][2][c]=curRewards.get(ub);
				cumRewards[d][1][c]=curCum.get(lb); cumRewards[d][2][c]=curCum.get(ub);
			}
		}
		//discounted rewards
		if(discounted){
			for(int d=0; d<numDim; d++){
				for(int c=0; c<maxCyclesGlobal; c++){
					ArrayList<Double> curRewards= new ArrayList<Double>();
					ArrayList<Double> curCum= new ArrayList<Double>();
					double mean=0, meanCum=0, denom=0;
					for(int t=0; t<numTraces; t++){
						if(maxCyclesLocal[t]>c){ //cycle observed
							double val=traces[t].cycleRewardsDis[d].get(c), valCum=traces[t].cumRewardsDis[d].get(c);
							curRewards.add(val); curCum.add(valCum);
							mean+=val; meanCum+=valCum; denom++;
						}
					}
					cycleRewardsDis[d][0][c]=mean/denom; cumRewardsDis[d][0][c]=meanCum/denom;
					Collections.sort(curRewards); Collections.sort(curCum);
					int bounds[]=getBoundIndices(denom); int lb=bounds[0], ub=bounds[1];
					cycleRewardsDis[d][1][c]=curRewards.get(lb); cycleRewardsDis[d][2][c]=curRewards.get(ub);
					cumRewardsDis[d][1][c]=curCum.get(lb); cumRewardsDis[d][2][c]=curCum.get(ub);
				}
			}
		}
		
		//build table
		for(int t=0; t<maxCyclesGlobal; t++){
			modelTraceRaw.addRow(new Object[]{null}); modelTraceRounded.addRow(new Object[]{null});
			int curCol=0;
			modelTraceRaw.setValueAt(t, t, curCol); modelTraceRounded.setValueAt(t, t, curCol); //cycle
			curCol++;
			for(int s=0; s<numStates; s++){
				for(int i=0; i<3; i++){
					modelTraceRaw.setValueAt(prev[s][i][t], t, curCol);
					modelTraceRounded.setValueAt(prev[s][i][t], t, curCol);
					curCol++;
				}
			}
			for(int d=0; d<numDim; d++){
				for(int i=0; i<3; i++){
					modelTraceRaw.setValueAt(cycleRewards[d][i][t],t,curCol);
					modelTraceRounded.setValueAt(myModel.round(cycleRewards[d][i][t],d),t,curCol);
					curCol++;
				}
			}
			for(int d=0; d<numDim; d++){
				for(int i=0; i<3; i++){
					modelTraceRaw.setValueAt(cumRewards[d][i][t],t,curCol);
					modelTraceRounded.setValueAt(myModel.round(cumRewards[d][i][t],d),t,curCol);
					curCol++;
				}
			}
			//discounted
			if(discounted){
				for(int d=0; d<numDim; d++){
					for(int i=0; i<3; i++){
						modelTraceRaw.setValueAt(cycleRewardsDis[d][i][t],t,curCol);
						modelTraceRounded.setValueAt(myModel.round(cycleRewardsDis[d][i][t],d),t,curCol);
						curCol++;
					}
				}
				for(int d=0; d<numDim; d++){
					for(int i=0; i<3; i++){
						modelTraceRaw.setValueAt(cumRewardsDis[d][i][t],t,curCol);
						modelTraceRounded.setValueAt(myModel.round(cumRewardsDis[d][i][t],d),t,curCol);
						curCol++;
					}
				}
			}
			//record num sims observed by cycle
			modelTraceRaw.setValueAt(numSims[t], t, curCol); modelTraceRounded.setValueAt(numSims[t], t, curCol);
		}
		
	}

	private int[] getBoundIndices(double dNum){
		int num=(int)dNum;
		int lb=(int) Math.round(0.025*num)-1;
		int ub=(int) Math.round(0.975*num)-1;
		lb=Math.max(0, lb); ub=Math.max(0, lb); //floor of 0
		lb=Math.min(num-1, lb); ub=Math.min(num-1, ub); //ceiling of num-1
		if(lb>=ub){ //set to min and max
			lb=0; ub=num-1;
		}
		return(new int[]{lb,ub});
	}
	
	

}