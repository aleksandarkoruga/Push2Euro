//////////////////////////////////////////////////////////////////////////////////////////////////////////
//     PUSH INTERFACE, classes for sequencer and MIDI processing 
//     
//		Reuse as you wish
//	
//     Aleksandar Koruga 2018 
//////////////////////////////////////////////////////////////////////////////////////////////////////////
import java.util.Arrays;

import com.cycling74.max.*;



public class Push2Euro01 extends MaxObject{

  
	//time variables
public double Global=0,PrevGlobal=0,Prevf=0;   
    //sequencers
public sequence[] SQ=new sequence[4];
public int seqE=0,pitchE=0;
public int stepE=0,pbE=0;
public int active=0;
public int selSeq=0;
public double scale=1./127.;	
	public Push2Euro01(){
		declareInlets(new int[]{DataTypes.FLOAT,DataTypes.LIST,DataTypes.LIST,DataTypes.FLOAT,DataTypes.FLOAT});
		declareOutlets(new int[]{DataTypes.FLOAT,DataTypes.FLOAT,DataTypes.FLOAT,DataTypes.ALL,DataTypes.ALL,DataTypes.FLOAT});
		
		setOutletAssist(new String[] { 
				"trig","pitch","voice_number","note","cc"});
		for(int i=0;i<4;i++)
		{
			SQ[i]=new sequence();
		}
		
	}
	///////////////////INPUTS////////////////////////////////////////////
	public void inlet(float f)
	{ // TIME FROM ABLETON
		int in=getInlet();
		if(in==4){
			Global=0;
			PrevGlobal=0;
			
		}
		if(in==3){
			scale=f;
			
		}
		if(in==0){
			//get clock
			
			Global =Math.floor(Global)+ f;
			
			//IF NEW QUARTER, THEN UPDATE GLOBAL
			if(f-Prevf<0)
			{
				Global+=1;
				
			}
			Prevf=f;
			
		
			
			//////DO SEQUENCING//////////
			for(int i=0;i<4;i++)
			{
				SQ[i].newtime(Global,PrevGlobal,i);
			}
			
			
			///END SEQUENCING
			
			
			//STORE CURRENT GLOBAL TIME AS PREVIOUS
			PrevGlobal=Global;
			
			
			
			
		}
		
	}
	public void list(Atom[] s)
	{
	
		
		int in=getInlet();
		
		
		//MIDI IN FROM PUSH 1-notes 2-cc
		if(in==1 && s.length==2)
		{
			
			if(s[0].isInt() && s[1].isInt() && s[1].getInt()!=0) // switching for notes editing, 
			{
				//note edit
				
				if(s[0].getInt()>=84 && s[0].getInt()<=99)
				{
					
					int nota=s[0].getInt()-84;
					int tempn=SQ[seqE].seq[nota][selSeq];
					if(stepE==0){
						
						
						SQ[seqE].seq[nota][selSeq]= tempn!=-1 ? (1-tempn):-1 ;   //[step number][bank]
						
						}
					else
					{
						 tempn=SQ[seqE].seq[nota][selSeq];
						
						SQ[seqE].seq[nota][selSeq]= tempn==-1 ? 1:-1 ;	
					}
					
					
					switch(tempn)
					{
					case -1:{tempn =0;}break;
					case 0:{ tempn =64; }break;
					case 1: {tempn=(int)(1+125.*SQ[seqE].getpitch(nota,selSeq));} break; 
					}
					
					SQ[seqE].outputSingleNT(s[0].getInt(),  (double) tempn  );
					SQ[seqE].outputSeq(selSeq);
				}
				
				//bank edit
				
				if(s[0].getInt()>=36 && s[0].getInt()<=83)
				{
					int bnkd=s[0].getInt()-36;
					int blocation= bnkd%8; //bank number
					int sq=((bnkd)/8);
					
					if(SQ[seqE].banks[blocation]!= sq )
					{
						
						SQ[seqE].outputSingleNT(36+blocation+8*SQ[seqE].banks[blocation],0);
						
						SQ[seqE].banks[blocation]=sq;
						//0 to changed bank
						SQ[seqE].outputSingleNT(s[0].getInt(),64.);
						
						
						
					}
					else{
						SQ[seqE].banks[blocation]=-1; //turn off bank
						
						SQ[seqE].outputSingleNT(s[0].getInt(),0);
						
						
					}
					
					
				}
				
				
				
				
				
			}
			
			
			
		}
		if(in==2 && s.length==2)
		{
			if(s[0].isInt() && s[1].isInt() && s[1].getInt()!=0) // switching for cc editing, 
			{
				switch (s[0].getInt())
				{
				case 44: {seqE=0; SQ[seqE].outputAll();   }         // change edited sequence-> also output sequencer state
				   break;
				case 45: {seqE=1; SQ[seqE].outputAll();   }
				   break;
				case 46: {seqE=2; SQ[seqE].outputAll();	  }
				   break;
				case 47: {seqE=3; SQ[seqE].outputAll();   }
				   break;
				case 43:{ pitchE=1; SQ[seqE].outputSingleCC(42,0.);SQ[seqE].outputSingleCC(43,120.);	}		// pitch edit first row 43 second row 42
				   break;
				case 42:{ pitchE=0; SQ[seqE].outputSingleCC(43,0.);SQ[seqE].outputSingleCC(42,120.);	}	
				
				   break;
				case 20: {stepE=0;	SQ[seqE].outputSingleCC(21,0.);SQ[seqE].outputSingleCC(20,120.);	}	// step edit beats 20, onoff 21
				   break;
				case 21: {stepE=1;  SQ[seqE].outputSingleCC(20,0.); SQ[seqE].outputSingleCC(21,120.);	}
				   break;
				case 102:{ pbE=0;	SQ[seqE].outputSingleCC(103,0.);SQ[seqE].outputSingleCC(102,120.);	}	// pitch or bank length 0 pitch 1 bank
				   break;
				case 103:{ pbE=1;	SQ[seqE].outputSingleCC(102,0.);SQ[seqE].outputSingleCC(103,120.);	}
				
				   break;
				case 71:   					// update pitch or bank length
				case 72:
				case 73:
				case 74:	
				case 75:
				case 76:
				case 77:
				case 78:
				{ 
					int vl=s[1].getInt();
					if(vl>64)
					{ vl= -1*(128-vl);   }
				//	outlet(5,vl);
					
					if(pbE==0){
					
						
						SQ[seqE].setpitch(s[0].getInt()-71+pitchE*8, selSeq,	Math.max( Math.min(  SQ[seqE].getpitch(s[0].getInt()-71+pitchE*8, selSeq)+ scale*(double)vl,1.),0.)	);			//todo: output pitch change on push
						//////////////fa l'output della nota cambiata di pitch
						if(SQ[seqE].seq[s[0].getInt()-71+pitchE*8][selSeq]==1){
						SQ[seqE].outputSingleNT(s[0].getInt()-71+pitchE*8+84,1+125.*SQ[seqE].getpitch(s[0].getInt()-71+pitchE*8, selSeq));
						}
					}
					if(pbE==1 && Math.abs(vl)==1){
						int tn=(int)  Math.max( Math.min(  (double) SQ[seqE].LengthFactor[s[0].getInt()-71]+(double)Math.signum(vl),8.),1.);
						SQ[seqE].LengthFactor[s[0].getInt()-71]= tn;
						
						
						if(tn <5){tn +=105; }else{tn+=19 ;}
						for(int i=0;i<8;i++)
						{
							if(i <4){
								SQ[seqE].outputSingleCC(i+106,0);}else{SQ[seqE].outputSingleCC(i+20,0);}
						}
						
						SQ[seqE].outputSingleCC(tn,35);
					}
					
					
					
				}			
					
				
				
				   break;
				
				
				case 36:
				case 37:	
				case 38:
				case 39:
				case 40:
				case 41:  {	selSeq=	(s[0].getInt()-36); SQ[seqE].outputSeq(selSeq);for(int i=0;i<6;i++){SQ[seqE].outputSingleCC(36+i,0);}SQ[seqE].outputSingleCC(36+selSeq,120);   }			//display selected sequence and prepeare for editing
				
				   break;
				case 59: {	active+=1;active%=2;  if (active==1){SQ[seqE].outputAll();}}	//USER MODE change output ALL 	
				   break;	
					
				 default: 
                 break;
				}
				
				
				
				
				
				
				
			}
		}
		
	}

	
////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////CLASS FOR STORING SEQUENCES//////////////////////
	
	public class sequence
	{
		double timeoffset;
		 int[][] seq;
		 double[][] pitches;
		 int [] banks;
		 int activeBank;
		 int[] LengthFactor;
		 int activeStep;
		 int[] stepbank;
		 
		 sequence(){     //VOID CONSTRUCTOR
					this.seq=new int [16][6];
					this.pitches=new double[16][6];	
					this.banks=new int[8];
					this.activeBank=0;
					this.activeStep=0;
					this.timeoffset=0;
					this.LengthFactor=new int[8]; // subdivision of a bar
				    Arrays.fill(this.LengthFactor, 1);
				    
				   }
			
		 
		 public void newtime(double curr,double prev,int snr)
		 {
			 ///// UPDATE NEW STEPS IF NECESSARY
			double to=curr;
			curr-=this.timeoffset;
			prev-=this.timeoffset;
			double divt=(double)this.LengthFactor[this.activeBank];
			curr%=divt;
			prev%=divt;
			curr/=divt;
			prev/=divt;
			
			
			if( curr-prev>0){  //STEP GOES FORWARD
			 
			int tempo= nsteps(this.banks[this.activeBank]);
			if(tempo!=0)
			{	
				
			double invt= 1./ (double) tempo;
			double tcurr=curr%invt;
			double tprev=prev%invt;
			
			if(tcurr-tprev<0   )
			{
				this.activeStep++;
				this.outputGP(this.activeStep,this.banks[this.activeBank],snr);
				
				
				
				
				
				
				
			}
			
			}
			
			}
			else  // bank goes forward
			{
				this.activeBank+=1;
				this.activeBank%=8;
				int j=0;
				while(this.banks[this.activeBank]==-1)  //-1 bank not active
				{
					this.activeBank+=1;
					this.activeBank%=8;
					j++;
					if(j==8){this.activeBank=0; break;}
				}
				
				this.activeStep=0;
				this.outputGP(this.activeStep,this.banks[this.activeBank],snr);
				this.timeoffset=to;
			}
			
			
			 
		 }
		 
		 
		 
		 public void setpitch(int stp,int bnk,double val)
		{
			this.pitches[stp][bnk]=val;
			
		}
		
		public double getpitch(int stp,int bnk)
		{
			return this.pitches[stp][bnk];
		}
		
		
		
		public int nsteps(int inbank)
		{
			if(inbank>=0 && inbank<6)return this.calculateactive(this.seq,inbank);
			else return 0;
		}
		
		
		////////////////
		public void outputAll()    // refresh push
		{
			this.outputSeq(selSeq);
			for(int i=36;i<84;i++)
			{
				this.outputSingleNT(i,0.);
			}
			for(int i=0;i<8;i++)
			{
				this.outputSingleNT(36+i+this.banks[i]*8,64);
				
			}
			
			for(int i=0;i<6;i++){outputSingleCC(36+i,0);}
			outputSingleCC(36+selSeq,120);
			
//////////////////////////
			this.outputSingleCC(42,0.+pitchE);this.outputSingleCC(43,0.+pitchE);
			
			for(int i=0;i<4;i++){this.outputSingleCC(44+i,0);}
			this.outputSingleCC(44+seqE,120);
			this.outputSingleCC(102,0.+stepE);this.outputSingleCC(103,0.+stepE);


			
			
		}
		public void outputSingleCC(int msg,double val)
		{
			//outlet 4
			Atom[] at;
			at=new Atom[]{Atom.newAtom(msg),Atom.newAtom((int)0)};
			outlet(4,at);
			at=new Atom[]{Atom.newAtom(msg),Atom.newAtom((int)val)};
			outlet(4,at);
			
			
		}
		public void outputSingleNT(int msg,double val)
		{
			//outlet 3
			Atom[] at;
			at=new Atom[]{Atom.newAtom(msg),Atom.newAtom((int)0)};
			outlet(3,at);
			at=new Atom[]{Atom.newAtom(msg),Atom.newAtom((int)val)};
			outlet(3,at);
			
		}
		public void outputSeq(int seqN)	// output sequence on push
		{
			Atom[] at;
			for(int i=0;i<16;i++)
			{
				int tval=this.seq[i][seqN];
				int ou=0;
				if(tval==-1){ou=0;}
				if(tval==0){ou=64;}
				if(tval==1){ou= (int)(this.getpitch(i,seqN)*125.+1); }
				
				at=new Atom[]{Atom.newAtom(84+i),Atom.newAtom((int)0)};
				outlet(3,at);	
				
				at=new Atom[]{Atom.newAtom(84+i),Atom.newAtom(ou)};
				outlet(3,at);	
			}
			
		}
		
		/////////////////PRIVATE METHODS/////////////////////////////////////
		
		private int calculateactive(int[][] inseq,int inbnk)
		{
			int res=0;
			for(int i=0;i<16;i++)
			{
				if (inseq[i][inbnk]!=-1){res++;}
				
				
			}
			return res;
		}
		
		private void outputGP(int stp,int bnk,int snr)
		{
			if((stp>-1) && (bnk>-1)){
			int location=0;
			int counter=-1;
			for(int i=0;i<16;i++)
			{
				if(this.seq[i][bnk]!=-1)
				{counter++;}
				
				
				if(counter==stp)
				{location=i;
				break;}
				
				
			}
			
			double ptch = this.getpitch(location, bnk);
			
			if(snr==seqE)this.outputSeq(selSeq);
			
			if(snr==seqE && bnk==selSeq)
			{
				
				this.outputSeq(bnk);
				
				
				
				SQ[seqE].outputSingleNT(location+84,0.);
				
				
				/*if((SQ[seqE].seq[location][bnk])==0)
				{
					
					SQ[seqE].outputSingleNT(location+84,64.);
				}
			
			
				if(  SQ[seqE].seq[location][bnk]==1  )
				{

				SQ[seqE].outputSingleNT(location+84,1.+125.*SQ[seqE].getpitch(location, bnk));
				
				}*/
				
			}
			///// ADD PUSH OUTPUT
			
			outlet(2,(float)snr);
			if(this.seq[location][bnk]==1){
			outlet(1,(float)ptch);}
			outlet(0,(float)this.seq[location][bnk]);
			}
			
		}
		
		
		
		
		
	}


}
	
	


	

