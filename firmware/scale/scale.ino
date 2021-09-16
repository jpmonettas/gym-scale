
#define dataPin  13
#define clockPin 12
#define AVERAGE_TIMES 10

enum scale_status {UNCALIBRATED=0, READY=1};

long  _offset   = 8779950; // use tare() to find this value
float _scale    = -0.04536540; // use calibrate_scale(weight) to find this value

int status = UNCALIBRATED;

long read() 
{
  // this waiting takes most time...
  while (digitalRead(dataPin) == HIGH) yield(); 

  long value = 0;
  

  noInterrupts();

  for (int i=0;i<24;i++){
    digitalWrite(clockPin, HIGH);
    value=value<<1;
    digitalWrite(clockPin, LOW);
    if(digitalRead(dataPin) == HIGH) value++;
  } 

  digitalWrite(clockPin, HIGH);
  value=value^0x800000;
  digitalWrite(clockPin, LOW);
    
  interrupts();

  return value;
}

float get_scaled()
{
  float units = read();
  return (units - _offset) * _scale;
};

void tare(){
  _offset = read();
  
  status=READY;
  send_status();
}

void calibrate_scale(int weight) // in grams
{ 
  long avg = read();
  _scale = (1.0 * weight) / (avg - _offset);
  status=READY;
  send_status();
  Serial.print("_scale :"); Serial.println(_scale, 8);
}

void setup()
{
  // Serial setup
  Serial.begin(115200);

  // HX711 Pins setup 
  pinMode(dataPin, INPUT);
  pinMode(clockPin, OUTPUT);
  digitalWrite(clockPin, LOW);

  tare();
}

void read_and_execute_command(){
  char cmd_buffer[20];
  size_t bytes_read;
  
  if(Serial.available() > 0) {
    
	bytes_read = Serial.readBytesUntil('\n', cmd_buffer, 20);
  cmd_buffer[bytes_read] = '\0';
  
	char cmd_name[5];
	char cmd_arg[7]; // 6 dig string, like 200000 (200 Kg)
	
	// first four bytes are cmd name
  int i=0;	
	for(; i<4; i++){
	  cmd_name[i] = cmd_buffer[i];
	}
  cmd_name[i] = '\0';

  i++; // jump over space char
  
	// the rest of the bytes are the argument
  int j=0;
	while(i<bytes_read){
	  cmd_arg[j] = cmd_buffer[i];
    j++;
    i++;
	}
  cmd_arg[j]='\0';
  
	if(strcmp(cmd_name, "CALI") == 0){
    int weight=atoi(cmd_arg);
    calibrate_scale(weight);
	} else if(strcmp(cmd_name, "TARE") == 0) {
	  tare();
	}
	
  }
  
}

void send_status(){
  Serial.print("STATUS ");
  if(status == UNCALIBRATED){
    Serial.println("UNCALIBRATED");
  } else if (status == READY) {
    Serial.println("READY");
  }
}

void send_weight(){
   Serial.print("WEIGHT "); 
   Serial.println(get_scaled());
}

void loop()
{
  read_and_execute_command();
  
  if(status == UNCALIBRATED){
    send_status();
  }else if(status == READY){
    send_weight();
  }
  
  delay(500);
}
