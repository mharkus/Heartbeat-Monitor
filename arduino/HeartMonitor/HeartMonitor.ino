#include <Usb.h>
#include <AndroidAccessory.h>

AndroidAccessory acc("Marc Tan",
		     "Seeduino",
		     "Heart Monitor with Android",
		     "1.0",
		     "http://www.marctan.com",
		     "0000000012345678");

 
unsigned char pin = 13;
unsigned char counter=0;
unsigned int heart_rate=0;
unsigned long temp[21];
unsigned long sub=0;
volatile unsigned char state = LOW;
bool data_effect=true;
const int max_heartpluse_duty=2000;
 //you can change it follow your system's request.2000 meams 2 seconds. 
//System return error if the duty overtrip 2 second.

void setup();
void loop();
 
void setup()
{
  pinMode(pin, OUTPUT);
  
  Serial.begin(9600);
  acc.powerOn();
  
  Serial.println("Please ready your chest belt.");
  delay(5000);//
  array_init();
  Serial.println("Heart rate test begin.");
  attachInterrupt(0, interrupt, RISING);//set interrupt 0,digital port 2
}
void loop()
{
  if (acc.isConnected()) {
          digitalWrite(pin, state);
   }

  delay(50);
  
}
void sum()//calculate the heart rate
{
 if(data_effect)
    {
      heart_rate=1200000/(temp[20]-temp[0]);//60*20*1000/20_total_time 
      Serial.print("Heart Rate: ");
      Serial.println(heart_rate);
      
      byte msg[2];
      msg[0] = 1;
      msg[1] = heart_rate;
      acc.write(msg, 2);
    }
   data_effect=1;//sign bit
}
void interrupt()
{
    temp[counter]=millis();
    state = !state;    
    Serial.print("Received heartbeat: ");
    Serial.println(temp[counter]);
    
    byte msg[2];
    msg[0] = 0;
    msg[1] = temp[counter];
    acc.write(msg, 2);
    
    switch(counter)
      {
       case(0):
       sub=temp[counter]-temp[20];

       break;
       default:
       sub=temp[counter]-temp[counter-1];

       break;
      }
    if(sub>max_heartpluse_duty)//set 2 seconds as max heart pluse duty
      {
        data_effect=0;//sign bit
        counter=0;
        Serial.println("Heart rate measure error,test will restart!" );
        array_init();
       }
    if (counter==20&&data_effect)
    {
      counter=0;
      sum();
    }
    else if(counter!=20&&data_effect){
      counter++;
    }else 
    {
      counter=0;
      data_effect=1;
    }
}
void array_init()
{
  for(unsigned char i=0;i!=20;++i)
  {
    temp[i]=0;
  }
  temp[20]=millis();
}
