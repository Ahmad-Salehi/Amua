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

package export_R;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import base.AmuaModel;
import main.*;
import math.Constants;
import math.Distributions;
import math.Functions;
import math.Interpreter;
import math.MatrixFunctions;
import math.Numeric;
import math.NumericException;

public class RModel{
	String dir;
	BufferedWriter out;
	BufferedWriter outFx;
	AmuaModel myModel;
	ArrayList<String> functionNames; //to export
	ArrayList<String> functionMethods;
	
	public RModel(String dir, BufferedWriter out, AmuaModel myModel) throws IOException{
		this.dir=dir;
		this.out=out;
		this.myModel=myModel;
		functionNames=new ArrayList<String>();
		functionMethods=new ArrayList<String>();
		FileWriter fstream = new FileWriter(dir+"functions.R"); //Create new file
		outFx = new BufferedWriter(fstream);
	}

	public void writeProperties(){
		try{
			writeLine("\"");
			writeLine("This code was auto-generated by Amua (https://github.com/zward/Amua)");
			writeLine("Code generated: "+new Date());
			writeLine("Model name: "+myModel.name);
			if(myModel.type==0){writeLine("Model type: Decision Tree");}
			else if(myModel.type==1){writeLine("Model type: Markov Model");}
			if(myModel.simType==0){writeLine("Simulation type: Cohort");}
			else if(myModel.simType==1){writeLine("Simulation type: Monte Carlo");}
			//metadata
			writeLine("Created by: "+myModel.meta.author);
			writeLine("Created: "+myModel.meta.dateCreated);
			writeLine("Version created: "+myModel.meta.versionCreated);
			writeLine("Modified by: "+myModel.meta.modifier);
			writeLine("Modified: "+myModel.meta.dateModified);
			writeLine("Version modified: "+myModel.meta.versionModified);
			writeLine("\"");
		}catch(Exception e){
			e.printStackTrace();
			myModel.errorLog.recordError(e);
		}
	}


	public String translate(String expression, boolean personLevel) throws Exception{
		String curText=expression.replaceAll(" ", ""); //remove spaces
		String exportText="";
		//Parse expression word by word
		int len=curText.length();
		while(len>0){
			int pos=Interpreter.getNextBreakIndex(curText);
			String word=curText.substring(0, pos);
			String split="";
			if(pos<len){split=curText.substring(pos,pos+1);}
			
			if(myModel.isTable(word)){ //if table
				int tableIndex=myModel.getTableIndex(word);
				Table curTable=myModel.tables.get(tableIndex);
				if(curTable.type.matches("Lookup")){
					int close=Interpreter.findRightBracket(curText,pos);
					String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
					if(!curTable.interpolate.matches("Cubic Splines")){
						exportText+="lookupTable("+curTable.name+","+translate(args[0],personLevel)+","+curTable.getColumnIndex(args[1])+","; //table,index,col
						exportText+="\""+curTable.lookupMethod+"\",";
						exportText+="\""+curTable.interpolate+"\",";
						exportText+="\""+curTable.boundary+"\",";
						exportText+="\""+curTable.extrapolate+"\")";
					}
					else{ //cubic splines
						int col=curTable.getColumnIndex(args[1]);
						exportText+="lookupTable("+curTable.name+","+translate(args[0],personLevel)+","+col+","; //table,index,col
						exportText+="\""+curTable.lookupMethod+"\",";
						exportText+="\""+curTable.interpolate+"\",";
						exportText+="\""+curTable.boundary+"\",";
						exportText+="\""+curTable.extrapolate+"\",";
						exportText+=curTable.name+"_knots_"+col+",";
						exportText+=curTable.name+"_knotHeights_"+col+",";
						exportText+=curTable.name+"_splineCoeffs_"+col+",";
						exportText+=curTable.name+"_boundaryCondition_"+col+")";
					}
				
					pos=close; //Move to end of table indices
				}
				else if(curTable.type.matches("Distribution")){ //Replace with value
					int close=Interpreter.findRightParen(curText,pos);
					String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
					exportText+="calcTableEV("+curTable.name+","+translate(args[0],personLevel)+")";
					pos=close; //Move to end of dist parameters
				}
				else if(curTable.type.matches("Matrix")){
					if(pos<len && curText.charAt(pos)=='['){ //matrix index
						int close=Interpreter.findRightBracket(curText,pos);
						String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
						//add 1 for index 1 instead of index 0
						exportText+=curTable.name+"data["+translate(args[0],personLevel)+"+1,"+translate(args[1],personLevel)+"+1]";
						pos=close; //Move to end of matrix indices
					}
					else{ //entire matrix
						exportText+=curTable.name;
					}
				}
			}
			else if(myModel.isVariable(word)){ //Variable
				if(personLevel){ //individual-level
					exportText+="person."+word+"[p]"+split;
				}
				else{
					exportText+=word+split;
				}
			}
			else if(word.matches("trace")){ //Markov Trace
				int close=Interpreter.findRightBracket(curText,pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				String row=translate(args[0],personLevel);
				String col=translate(args[1],personLevel);
				if(col.charAt(0)=='\'' || col.charAt(0)=='\"'){ //String column reference
					col=col.substring(1, col.length()-1); //trim quotes
					exportText+="trace$"+col+"["+row+"+1]";
				}
				else{ //use numeric indices
					exportText+="trace["+row+"+1,"+col+"+1]";
				}
				pos=close; //Move to end of trace indices
			}
			else if(Functions.isFunction(word)){
				int close=Interpreter.findRightParen(curText, pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				int inPlace=RFunctions.inPlace(word);
				if(inPlace==0){ //translate in place
					exportText+=RFunctions.translate(word)+"("+translate(args[0],personLevel);
					for(int i=1; i<args.length; i++){exportText+=","+translate(args[i],personLevel);}
					exportText+=")";
					pos=close+1; //Move to end of function call
				}
				else if(inPlace==1){ //define function method
					exportText+=word+"("+translate(args[0],personLevel);
					for(int i=1; i<args.length; i++){exportText+=","+translate(args[i],personLevel);}
					exportText+=")";
					pos=close+1; //Move to end of function call
					if(!functionNames.contains(word)){ //not defined yet
						functionNames.add(word);
						functionMethods.add(RFunctions.define(word));
					}
				}
				else if(inPlace==2){ //change arguments
					exportText+=RFunctions.changeArgs(word,args,this,personLevel);
					pos=close; //Move to end of function indices
				}
			}
			else if(MatrixFunctions.isFunction(word)){
				int close=Interpreter.findRightParen(curText, pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				int inPlace=RMatrixFunctions.inPlace(word);
				if(inPlace==0){ //translate in place
					exportText+=RMatrixFunctions.translate(word)+"("+translate(args[0],personLevel);
					for(int i=1; i<args.length; i++){exportText+=","+translate(args[i],personLevel);}
					exportText+=")";
					pos=close+1; //Move to end of function call
				}
				else if(inPlace==1){ //define function method
					exportText+=word+"("+translate(args[0],personLevel);
					for(int i=1; i<args.length; i++){exportText+=","+translate(args[i],personLevel);}
					exportText+=")";
					pos=close+1; //Move to end of function call
					if(!functionNames.contains(word)){ //not defined yet
						functionNames.add(word);
						functionMethods.add(RMatrixFunctions.define(word));
					}
				}
				else if(inPlace==2){ //change arguments
					exportText+=RMatrixFunctions.changeArgs(word,args,this,personLevel);
					pos=close; //Move to end of matrix indices
				}
			}
			else if(Distributions.isDistribution(word)){
				//just parse arguments for now
				int close=Interpreter.findRightParen(curText,pos);
				String args[]=Interpreter.splitArgs(curText.substring(pos+1,close));
				exportText+=word+"("+translate(args[0],personLevel);
				for(int i=1; i<args.length; i++){
					exportText+=","+translate(args[i],personLevel);
				}
				exportText+=")";
				pos=close+1; //Move to end of distribution call
			}
			else if(Constants.isConstant(word)){
				exportText+=RConstants.translate(word)+split;
			}
			else if(curText.charAt(0)=='['){ //matrix
				int close=Interpreter.findRightBracket(curText,0);
				String strMatrix=curText.substring(1,close);
				Numeric matrix=Interpreter.parseMatrix(strMatrix,myModel,false);
				//write out
				if(matrix.nrow>1){exportText+=writeMatrix(matrix.matrix);}
				else{exportText+=writeArray(matrix.matrix[0]);}
				pos=close+1; //Move to end of matrix
			}
			else{ //not key word
				exportText+=word+split;
			}


			if(pos==len){len=0;} //End of word
			else{
				curText=curText.substring(pos+1);
				len=curText.length();
			}
		}

		return(exportText);
	}
	
	private String initNumeric(String name, Numeric value) throws NumericException{
		String init="";
		if(value.isDouble() || value.isInteger()){init=name+"="+value.getDouble();}
		else if(value.isBoolean()){init=name+"="+value.toString().toUpperCase();}
		else if(value.isMatrix()){
			if(value.nrow>1){init=name+"<-"+writeMatrix(value.matrix);}
			else{init=name+"<-"+writeArray(value.matrix[0]);}
		}
		return(init);
	}
	
	public void writeParameters() throws NumericException{
		int numParams=myModel.parameters.size();
		if(numParams>0){
			writeLine("###Define parameters");
			for(int i=0; i<numParams; i++){
				Parameter curParam=myModel.parameters.get(i);
				if(!curParam.notes.isEmpty()){writeLine("\""+curParam.notes+"\"");}
				String expr=curParam.expression;
				String init=initNumeric(curParam.name,curParam.value);
				writeLine(init+" #Expression: "+expr);
			}
			writeLine("");
		}
	}
	
	public void writeVariables() throws NumericException{
		int numVars=myModel.variables.size();
		if(numVars>0){
			writeLine("###Define variables");
			for(int i=0; i<numVars; i++){
				Variable curVar=myModel.variables.get(i);
				if(!curVar.notes.isEmpty()){writeLine("\""+curVar.notes+"\"");}
				String init=initNumeric(curVar.name,curVar.value);
				writeLine(init);
			}
			writeLine("");
		}
	}

	public void writeTables(int format){
		try{
			int numTables=myModel.tables.size();
			if(numTables>0){
				writeLine("###Define tables");
				if(format==0){ //Inline
					for(int i=0; i<numTables; i++){
						Table curTable=myModel.tables.get(i);
						writeLine(curTable.name+"<-data.frame(matrix(nrow="+curTable.numRows+",ncol="+curTable.numCols+"))");
						out.write("names("+curTable.name+")<-c(");
						for(int c=0; c<curTable.numCols-1; c++){out.write("\""+curTable.headers[c]+"\",");}
						out.write("\""+curTable.headers[curTable.numCols-1]+"\")"); out.newLine();
						for(int r=0; r<curTable.numRows; r++){
							out.write(curTable.name+"["+(r+1)+",]<-c(");
							for(int c=0; c<curTable.numCols-1; c++){out.write(curTable.data[r][c]+",");}
							out.write(curTable.data[r][curTable.numCols-1]+")"); out.newLine();
						}
						if(curTable.interpolate!=null && curTable.interpolate.matches("Cubic Splines")){
							writeTableSplines(curTable);
						}
						writeLine("");
					}
					writeLine("");
				}
				else if(format==1){ //CSV
					writeLine("setwd(\""+dir.replaceAll("\\\\", "\\\\\\\\")+"\")");
					for(int i=0; i<numTables; i++){
						Table curTable=myModel.tables.get(i);
						writeLine(curTable.name+"<-read.csv(\""+curTable.name+".csv\")");
						if(curTable.interpolate!=null && curTable.interpolate.matches("Cubic Splines")){
							writeTableSplines(curTable);
						}
					}
					writeLine("");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			myModel.errorLog.recordError(e);
		}
	}

	private void writeTableSplines(Table curTable) throws IOException{
		int numSplines=curTable.splines.length;
		for(int s=0; s<numSplines; s++){
			writeLine(curTable.name+"_knots_"+(s+1)+"<-"+writeArray(curTable.splines[s].knots));
			writeLine(curTable.name+"_knotHeights_"+(s+1)+"<-"+writeArray(curTable.splines[s].knotHeights));
			writeLine(curTable.name+"_splineCoeffs_"+(s+1)+"<-"+writeMatrix(curTable.splines[s].splineCoeffs));
			writeLine(curTable.name+"_boundaryCondition_"+(s+1)+"<-"+(curTable.splines[s].boundaryCondition));
		}
	}
	
	private String writeArray(double array[]){
		int len=array.length;
		String write="c(";
		for(int i=0; i<len-1; i++){write+=array[i]+",";}
		write+=array[len-1]+")";
		return(write);
	}
	
	private String writeMatrix(double matrix[][]){
		int nrow=matrix.length;
		String write="rbind(";
		write+=writeArray(matrix[0]);
		for(int r=1; r<nrow; r++){
			write+=","+writeArray(matrix[1]);
		}
		write+=")";
		return(write);
	}
	
	/**
	 * Defines any function methods needed
	 */
	public void defineFunctions(){
		try{
			int numFx=functionNames.size();
			for(int f=0; f<numFx; f++){
				outFx.write(functionMethods.get(f)); outFx.newLine();
				outFx.newLine();
			}
			outFx.close();
		}catch(Exception e){
			e.printStackTrace();
			myModel.errorLog.recordError(e);
		}
	}
	
	/**
	 * Defines table functions in R
	 * @param out
	 */
	public void writeTableClass(){
		int numTables=myModel.tables.size();
		if(numTables>0){
			BufferedWriter temp=out; //re-point
			out=outFx;
			
			writeLine("### Define Table Functions");
			writeLine("cubicSplines<-function(x,knots,knotHeights,splineCoeffs,boundaryCondition){");
			writeLine("	numKnots=length(knots)");
			writeLine("	y=NaN");
			writeLine("	#Find domain");
			writeLine("	index=-1");
			writeLine("	if(x<knots[1]){ #Extrapolate left");
			writeLine("		x=x-knots[1]");
			writeLine("		a=splineCoeffs[1,]");
			writeLine("		if(boundaryCondition==1 | boundaryCondition==1){ #Natural or clamped");
			writeLine("			slope=a[2]");
			writeLine("			y=slope*x+knotHeights[1]");
			writeLine("		}");
			writeLine("		else{ #Not-a-knot or periodic");
			writeLine("			index=1");
			writeLine("			y=splineCoeffs[index,1]+splineCoeffs[index,2]*x+splineCoeffs[index,3]*x*x+splineCoeffs[index,4]*x*x*x");
			writeLine("		}");
			writeLine("	}");
			writeLine("	else if(x>knots[numKnots]){ #Extrapolate right");
			writeLine("		a=splineCoeffs[numKnots-1]");
			writeLine("		if(boundaryCondition==1 | boundaryCondition==1){ #Natural or clamped");
			writeLine("			x=x-knots[numKnots]");
			writeLine("			h=knots[numKnots]-knots[numKnots-1]");
			writeLine("			slope=a[2]+2*a[3]*h+3*a[4]*h*h");
			writeLine("			y=slope*x+knotHeights[numKnots]");
			writeLine("		}");
			writeLine("		else{ #Not-a-knot or periodic");
			writeLine("			index=numKnots-1");
			writeLine("			x=x-knots[index]");
			writeLine("			y=splineCoeffs[index,1]+splineCoeffs[index,2]*x+splineCoeffs[index,3]*x*x+splineCoeffs[index,4]*x*x*x");
			writeLine("		}");
			writeLine("	} else{ #Interpolate");
			writeLine("		index=1");
			writeLine("		while(x>knots[index+1] & index<numKnots-1){index=index+1}");
			writeLine("		x=x-knots[index]");
			writeLine("		y=splineCoeffs[index,1]+splineCoeffs[index,2]*x+splineCoeffs[index,3]*x*x+splineCoeffs[index,4]*x*x*x");
			writeLine("	}");
			writeLine("	return(y)");
			writeLine("}");
			writeLine("");
			writeLine("lookupTable<-function(data,index,col,lookupMethod,interpolate,boundary,extrapolate,knots,knotHeights,splineCoeffs,boundaryCondition){");
			writeLine("	col=col+1 #Index from 1");
			writeLine("	numRows=nrow(data)");
			writeLine("	if(col<2 || col>ncol(data)){return(NaN)} #Invalid column");
			writeLine("	else{ #Valid column");
			writeLine("		val=NaN");
			writeLine("		if(lookupMethod==\"Exact\"){");
			writeLine("			row=0");
			writeLine("			found=F");
			writeLine("			while(found==F & row<numRows+1){");
			writeLine("				row=row+1");
			writeLine("				if(index==data[row,1]){found=T}");
			writeLine("			}");
			writeLine("			if(found){val=data[row,col]}");
			writeLine("		}");
			writeLine("		else if(lookupMethod==\"Truncate\"){");
			writeLine("			if(index<data[1,1]){val=NaN} #Below first value - error");
			writeLine("			else if(index>=data[numRows,1]){val=data[numRows,col]} #Above last value");
			writeLine("			else{ #Between");
			writeLine("				row=1");
			writeLine("				while(data[row,1]<index){row=row+1}");
			writeLine("				if(index==data[row,1]){val=data[row,col]}");
			writeLine("				else{val=data[row-1,col]}");
			writeLine("			}");
			writeLine("		}");
			writeLine("		else if(lookupMethod==\"Interpolate\"){");
			writeLine("			if(interpolate==\"Linear\"){");
			writeLine("				if(index<=data[1,1]){ #Below or at first index");
			writeLine("					slope=(data[2,col]-data[1,col])/(data[2,1]-data[1,1])");
			writeLine("					val=data[1,col]-(data[1,1]-index)*slope");
			writeLine("				}");
			writeLine("				else if(index>data[numRows,1]){ #Above last index");
			writeLine("					slope=(data[numRows,col]-data[numRows-1,col])/(data[numRows,1]-data[numRows-1,1])");
			writeLine("					val=data[numRows,col]+(index-data[numRows,1])*slope");
			writeLine("				}");
			writeLine("				else{ #Between");
			writeLine("					row=1");
			writeLine("					while(data[row,1]<index){row=row+1}");
			writeLine("					slope=(data[row,col]-data[row-1,col])/(data[row,1]-data[row-1,1])");
			writeLine("					val=data[row-1,col]+(index-data[row-1,1])*slope");
			writeLine("				}");
			writeLine("			}");
			writeLine("			else if(interpolate==\"Cubic Splines\"){");
			writeLine("				val=cubicSplines(index,knots,knotHeights,splineCoeffs,boundaryCondition)");
			writeLine("			}");
			writeLine("");
			writeLine("			#Check extrapolation conditions");
			writeLine("			if(extrapolate==\"No\"){");
			writeLine("				if(index<=data[1,1]){val=data[1,col]} #Below or at first index");
			writeLine("				else if(index>data[numRows,1]){val=data[numRows,col]} #Above last index");
			writeLine("			}");
			writeLine("			else if(extrapolate==\"Left only\"){ #truncate right");
			writeLine("				if(index>data[numRows,1]){val=data[numRows,col]} #Above last index");
			writeLine("			}");
			writeLine("			else if(extrapolate==\"Right only\"){ #truncate left");
			writeLine("				if(index<=data[1,1]){val=data[1,col]} #Below or at first index");
			writeLine("			}");
			writeLine("		}");
			writeLine("		return(val)");
			writeLine("	}");
			writeLine("}");
			writeLine("");
			writeLine("calcTableEV<-function(data,col){");
			writeLine("	col=col+1 #Index from 1");
			writeLine("	ev=0");
			writeLine("	for(r in 1:nrow(data)){");
			writeLine("		ev=ev+data[r,1]*data[r,col]");
			writeLine("	}");
			writeLine("	return(ev)");
			writeLine("}");
			writeLine("");
			
			outFx=out; //point back
			out=temp;
		}
	}

	private void writeLine(String line){
		try{
			out.write(line); out.newLine();
		}catch(Exception e){
			e.printStackTrace();
			myModel.errorLog.recordError(e);
		}
	}
}
