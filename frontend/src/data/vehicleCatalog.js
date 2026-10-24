// 台灣市面常見廠牌與大宗車型（依廠牌分組）。沒列到的車型，介面上可選「其他」手動輸入。
export const OTHER = '__other__'

export const vehicleCatalog = {
  Toyota: ['Corolla Cross', 'Corolla Altis', 'Yaris', 'Yaris Cross', 'Vios', 'RAV4', 'Camry', 'Sienta', 'Town Ace', 'C-HR', 'Prius', 'Alphard', 'Hilux', 'bZ4X'],
  Lexus: ['NX', 'RX', 'UX', 'ES', 'IS', 'LBX', 'LM'],
  Honda: ['CR-V', 'HR-V', 'Civic', 'Fit', 'Accord', 'Odyssey', 'Freed', 'Vezel'],
  Nissan: ['Kicks', 'Sentra', 'X-Trail', 'Tiida', 'Livina', 'Juke', 'Leaf', 'Serena'],
  Mazda: ['Mazda3', 'Mazda2', 'CX-5', 'CX-30', 'CX-60', 'Mazda6', 'MX-5'],
  Mitsubishi: ['Outlander', 'Eclipse Cross', 'Colt Plus', 'Delica', 'Zinger', 'Lancer'],
  Suzuki: ['Swift', 'Jimny', 'Vitara', 'SX4', 'Ignis', 'Carry'],
  Subaru: ['Forester', 'XV', 'Outback', 'Impreza', 'Crosstrek', 'WRX', 'BRZ', 'Levorg'],
  Hyundai: ['Tucson', 'Venue', 'Kona', 'Santa Fe', 'Elantra', 'Ioniq 5', 'Custin'],
  Kia: ['Sportage', 'Sorento', 'Picanto', 'Stonic', 'Niro', 'EV6', 'Carnival'],
  Ford: ['Focus', 'Kuga', 'Fiesta', 'Ranger', 'Mustang', 'Explorer', 'Territory'],
  Volkswagen: ['Golf', 'Tiguan', 'Polo', 'T-Roc', 'Passat', 'Touran', 'Caddy'],
  BMW: ['1 系列', '2 系列', '3 系列', '5 系列', 'X1', 'X3', 'X5', 'iX3'],
  'Mercedes-Benz': ['A-Class', 'C-Class', 'E-Class', 'GLA', 'GLC', 'GLE', 'CLA'],
  Audi: ['A3', 'A4', 'A6', 'Q3', 'Q5', 'Q7', 'e-tron'],
  Volvo: ['XC40', 'XC60', 'XC90', 'S60', 'V60'],
  Porsche: ['Macan', 'Cayenne', '911', 'Taycan', 'Panamera'],
  MINI: ['Hatch', 'Countryman', 'Clubman', 'Cooper SE'],
  Tesla: ['Model 3', 'Model Y', 'Model S', 'Model X'],
  Luxgen: ['U6', 'URX', 'n7', 'S3', 'M7'],
  Skoda: ['Octavia', 'Kamiq', 'Karoq', 'Kodiaq', 'Fabia'],
  Peugeot: ['208', '2008', '308', '3008', '5008'],
  'Land Rover': ['Defender', 'Discovery Sport', 'Range Rover Evoque', 'Range Rover Velar'],
  Jeep: ['Wrangler', 'Compass', 'Renegade', 'Grand Cherokee']
}

export const brandOptions = Object.keys(vehicleCatalog)
