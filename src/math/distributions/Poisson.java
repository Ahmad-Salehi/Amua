package math.distributions;

import org.apache.commons.math3.special.Gamma;

import math.MathUtils;
import math.Numeric;
import math.NumericException;

public final class Poisson{
	
	public static Numeric pmf(Numeric params[]) throws NumericException{
		int k=params[0].getInt();
		double lambda=params[1].getDouble();
		if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
		double val=0;
		val=Math.exp(k*Math.log(lambda)-lambda-Gamma.logGamma(k+1));
		return(new Numeric(val));
	}

	public static Numeric cdf(Numeric params[]) throws NumericException{
		int k=params[0].getInt();
		double lambda=params[1].getDouble();
		if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
		double val=0;
		for(int i=0; i<=k; i++){
			val+=Math.exp(i*Math.log(lambda)-lambda-Gamma.logGamma(i+1));
		}
		return(new Numeric(val));
	}	
	
	public static Numeric quantile(Numeric params[]) throws NumericException{
		double x=params[0].getProb();
		double lambda=params[1].getDouble();
		if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
		if(x==1){return(new Numeric(Double.POSITIVE_INFINITY));}
		int k=-1;
		double CDF=0;
		while(x>CDF){
			double curMass=Math.exp((k+1)*Math.log(lambda)-lambda-Gamma.logGamma(k+2));
			CDF+=curMass;
			k++;
		}
		k=Math.max(0, k);
		return(new Numeric(k));
	}
	
	public static Numeric mean(Numeric params[]) throws NumericException{
		double lambda=params[0].getDouble();
		if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
		return(new Numeric(lambda));
	}
	
	public static Numeric variance(Numeric params[]) throws NumericException{
		double lambda=params[0].getDouble();
		if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
		return(new Numeric(lambda));
	}
	
	public static Numeric sample(Numeric params[], double rand) throws NumericException{
		if(params.length==1){
			double lambda=params[0].getDouble();
			if(lambda<=0){throw new NumericException("λ should be >0","Pois");}
			int k=-1;
			double CDF=0;
			while(rand>CDF){
				double curMass=Math.exp((k+1)*Math.log(lambda)-lambda-Gamma.logGamma(k+2));
				CDF+=curMass;
				k++;
			}
			return(new Numeric(k));
		}
		else{throw new NumericException("Incorrect number of parameters","Pois");}
	}
	
	public static String description(){
		String des="<html><b>Poisson Distribution</b><br>";
		des+="Used to model the number of events that occur in a fixed interval of time/space with a known average rate<br><br>";
		des+="<i>Parameters</i><br>";
		des+=MathUtils.consoleFont("λ")+": Average number of events in the interval ("+MathUtils.consoleFont(">0")+")<br>";
		des+="<br><i>Sample</i><br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(λ,<b><i>~</i></b>)")+": Returns a random variable (mean in base case) from the Poisson distribution. Integer in "+MathUtils.consoleFont("{0,1,...}")+"<br>";
		des+="<br><i>Distribution Functions</i><br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(k,λ,<b><i>f</i></b>)")+": Returns the value of the Poisson PMF at "+MathUtils.consoleFont("k")+"<br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(k,λ,<b><i>F</i></b>)")+": Returns the value of the Poisson CDF at "+MathUtils.consoleFont("k")+"<br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(x,λ,<b><i>Q</i></b>)")+": Returns the quantile (inverse CDF) of the Poisson distribution at "+MathUtils.consoleFont("x")+"<br>";
		des+="<br><i>Moments</i><br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(λ,<b><i>E</i></b>)")+": Returns the mean of the Poisson distribution<br>";
		des+=MathUtils.consoleFont("<b>Pois</b>","green")+MathUtils.consoleFont("(λ,<b><i>V</i></b>)")+": Returns the variance of the Poisson distribution<br>";
		des+="</html>";
		return(des);
	}
}