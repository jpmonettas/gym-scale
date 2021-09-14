EESchema Schematic File Version 4
EELAYER 30 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title ""
Date ""
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
Text Label 3580 4280 2    50   ~ 0
TopLeftRed
Text Label 3580 4160 2    50   ~ 0
TopLeftBlack
Text Label 3580 4390 2    50   ~ 0
TopLeftWhite
Text Label 3570 3830 2    50   ~ 0
TopRightRed
Text Label 3570 3930 2    50   ~ 0
TopRightBlack
Text Label 3570 3730 2    50   ~ 0
TopRightWhite
Text Label 3570 3570 2    50   ~ 0
BottomRightRed
Text Label 3570 3360 2    50   ~ 0
BottomRightBlack
Text Label 3570 3470 2    50   ~ 0
BottomRightWhite
Text Label 3570 2980 2    50   ~ 0
BottomLeftRed
Text Label 3570 3200 2    50   ~ 0
BottomLeftBlack
Text Label 3570 3090 2    50   ~ 0
BottomLeftWhite
Wire Wire Line
	3580 4160 3650 4160
Wire Wire Line
	3650 4160 3650 3930
Wire Wire Line
	3650 3930 3570 3930
Wire Wire Line
	3570 3360 3650 3360
Wire Wire Line
	3650 3360 3650 3200
Wire Wire Line
	3650 3200 3570 3200
Wire Wire Line
	3570 3830 3710 3830
Wire Wire Line
	3710 3830 3710 2980
Wire Wire Line
	3710 2980 3570 2980
Wire Wire Line
	3580 4280 3740 4280
Wire Wire Line
	3740 4280 3740 3570
Wire Wire Line
	3740 3570 3570 3570
$Comp
L custom:HX711 HX?
U 1 1 613B648A
P 5370 3640
F 0 "HX?" H 5370 4205 50  0000 C CNN
F 1 "HX711" H 5370 4114 50  0000 C CNN
F 2 "" H 5370 3640 50  0001 C CNN
F 3 "" H 5370 3640 50  0001 C CNN
	1    5370 3640
	-1   0    0    1   
$EndComp
Wire Wire Line
	3570 3470 3860 3470
Wire Wire Line
	3860 3470 3860 3640
Wire Wire Line
	3860 3640 4870 3640
Wire Wire Line
	3570 3730 4870 3730
Wire Wire Line
	4870 3730 4870 3740
Wire Wire Line
	3580 4390 3830 4390
Wire Wire Line
	3830 4390 3830 3840
Wire Wire Line
	3830 3840 4870 3840
Wire Wire Line
	3570 3090 3920 3090
Wire Wire Line
	3920 3090 3920 3940
Wire Wire Line
	3920 3940 4870 3940
NoConn ~ 4870 3440
NoConn ~ 4870 3540
$Comp
L dk_RF-Transceiver-Modules:ESP32-WROOM-32 MOD?
U 1 1 613B9ABF
P 7530 2870
F 0 "MOD?" H 7730 3173 60  0000 C CNN
F 1 "ESP32-WROOM-32" H 7730 3067 60  0000 C CNN
F 2 "digikey-footprints:ESP32-WROOM-32D" H 7730 3070 60  0001 L CNN
F 3 "https://www.espressif.com/sites/default/files/documentation/esp32-wroom-32_datasheet_en.pdf" H 7730 3170 60  0001 L CNN
F 4 "1904-1010-1-ND" H 7730 3270 60  0001 L CNN "Digi-Key_PN"
F 5 "ESP32-WROOM-32" H 7730 3370 60  0001 L CNN "MPN"
F 6 "RF/IF and RFID" H 7730 3470 60  0001 L CNN "Category"
F 7 "RF Transceiver Modules" H 7730 3570 60  0001 L CNN "Family"
F 8 "https://www.espressif.com/sites/default/files/documentation/esp32-wroom-32_datasheet_en.pdf" H 7730 3670 60  0001 L CNN "DK_Datasheet_Link"
F 9 "/product-detail/en/espressif-systems/ESP32-WROOM-32/1904-1010-1-ND/8544305" H 7730 3770 60  0001 L CNN "DK_Detail_Page"
F 10 "SMD MODULE, ESP32-D0WDQ6, 32MBIT" H 7730 3870 60  0001 L CNN "Description"
F 11 "Espressif Systems" H 7730 3970 60  0001 L CNN "Manufacturer"
F 12 "Active" H 7730 4070 60  0001 L CNN "Status"
	1    7530 2870
	1    0    0    -1  
$EndComp
Wire Wire Line
	5870 3840 5870 5220
Wire Wire Line
	5870 5220 7530 5220
Wire Wire Line
	7530 5220 7530 4970
Wire Wire Line
	5870 3540 5870 2770
Wire Wire Line
	5870 2770 7730 2770
Wire Wire Line
	5870 3640 6750 3640
Wire Wire Line
	6750 3640 6750 4770
Wire Wire Line
	6750 4770 7030 4770
Wire Wire Line
	5870 3740 6820 3740
Wire Wire Line
	6820 3740 6820 3670
Wire Wire Line
	6820 3670 7030 3670
$EndSCHEMATC
