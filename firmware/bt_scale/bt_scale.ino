#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BLEUUID.h>

#define dataPin  13
#define clockPin 12
#define AVERAGE_TIMES 10

// https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf
#define WEIGHT_SCALE_SERVICE_ID  0x181D
#define WEIGHT_CHARACTERISTIC_ID 0x2A98

// Scale vars
long  _offset   = 8779950; // use tare() to find this value
float _scale    = -0.04769912; // use calibrate_scale(weight) to find this value, this is calibrated using a 75.6 kgs
                  

// BLE vars

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint32_t value = 0;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

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
}

void calibrate_scale(int weight) // in grams
{ 
    Serial.print("Put on the scale "); Serial.print(weight); Serial.println(" grams and send any key");
    while(Serial.available() <= 0) {}
    Serial.read();
  long avg = read();
  _scale = (1.0 * weight) / (avg - _offset);
  Serial.print("_scale :"); Serial.println(_scale, 8);
}

void setup()
{
  
  // Serial setup
  Serial.begin(115200);

  // Scale HX711 Pins setup 
  pinMode(dataPin, INPUT);
  pinMode(clockPin, OUTPUT);
  digitalWrite(clockPin, LOW);

  tare();

  // Uncomment to calibrate
  //calibrate_scale(75600);

  BLEUUID service_id = BLEUUID((uint16_t)WEIGHT_SCALE_SERVICE_ID); 
  BLEUUID characteristic_id = BLEUUID((uint16_t)WEIGHT_CHARACTERISTIC_ID);
  
  // BLE Setup
  // Create the BLE Device
  BLEDevice::init("ESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(service_id);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      characteristic_id,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );

  pCharacteristic->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(service_id);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
  Serial.println("READY");
  
}

void loop()
{
  // notify changed value
    if (deviceConnected) {
        float weight = get_scaled();
        int weight_int = weight/5;
        uint16_t w = (uint16_t)weight_int;
//         Serial.print("Float:"); Serial.println(weight);
//         Serial.print("Int:"); Serial.println(weight_int);
//         Serial.print("UInt16:"); Serial.println(w);
//        
        pCharacteristic->setValue((uint16_t&)w);
        pCharacteristic->notify();
        delay(50); // bluetooth stack will go into congestion, if too many packets are sent
    }
    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
    }
}
