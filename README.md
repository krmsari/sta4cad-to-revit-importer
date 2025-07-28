# 🚀 STA4CAD'den Revit'e Model Aktarım Projesi

Bu proje, STA4CAD'de oluşturulan yapısal modellerin `.ST4` formatından okunarak Revit ortamına otomatik olarak aktarılmasını sağlayan bir araçtır. Proje, birbiriyle entegre çalışan iki ana bileşenden oluşur:

1.  **Java Backend Servisi:** `.ST4` dosyasını işleyerek okunurluğu olan standart bir JSON formatına dönüştürür.
2.  **C# Revit Eklentisi:** Oluşturulan JSON verisini okuyarak Revit içinde yapısal modeli (kolon, perde kolon, kiriş, döşeme vb.) inşa eder.

---


### Klasör Yapısı

Depo, bileşenleri net bir şekilde ayırmak için aşağıdaki gibi organize edilmiştir:

```
/sta4cad-to-revit-importer/
|
├── .gitignore          # Her iki proje türü için genel ignore kuralları
├── README.md           # Bu dosya - Projenin ana açıklaması
|
├── 📁 backend-java/       # Java Spring Boot projesinin kök dizini
│   ├── src/
│   ├── pom.xml
│   └── ... (Diğer Java/Spring proje dosyaları)
|
└── 📁 revit-addin-csharp/ # C# Revit eklentisinin kök dizini
    ├── v01.sln
    ├── v01/
    │   ├── v01.csproj
    │   ├── v01.addin
    │   └── ... (Diğer C# proje dosyaları)
    └── ...
```

---

## ⚙️ Kurulum ve Kullanım

### 1. Backend Servisi (Java) ☕

Bu servis, `.ST4` dosyasını alıp işler ve Revit eklentisinin anlayacağı bir `.JSON` dosyası oluşturur.

* **Gereksinimler:**
    * Java Development Kit (JDK) 11 veya üstü
    * Apache Maven

* **Derleme:**
    1.  Terminal veya komut istemcisini açın.
    2.  `backend-java` klasörüne gidin:
        ```bash
        cd backend-java
        ```
    3.  Projeyi derleyerek çalıştırılabilir `.jar` dosyasını oluşturun:
        ```bash
        mvn clean package
        ```
    4.  Derleme sonucunda `target/` klasörü altında `sta4cad-imp.jar` (veya `pom.xml`'de belirtilen başka bir isimde) dosyası oluşacaktır. Bu dosya, C# eklentisinin çalışması için **gereklidir**.

### 2. Revit Eklentisi (C#) 🏢

Bu eklenti, Java servsinin ürettiği `.JSON` dosyasını kullanarak Revit'te yapısal elemanları oluşturur.

* **Gereksinimler:**
    * Microsoft Visual Studio 2019 veya üstü
    * .NET Framework 4.8
    * Autodesk Revit 2025 veya üstü

#### Kurulum Adımları

**Adım 1: Eklentiyi Derleme (Build)**

Visual Studio'da `revit-addin-csharp/v01.sln` çözümünü açın ve projeyi `Build` edin. Bu işlem sonucunda `bin/Debug` (veya `bin/Release`) klasörü altında **`v01.dll`** dosyası oluşacaktır.

**Adım 2: Revit Manifest Dosyasını (`.addin`) Hazırlama**

Revit'in eklentinizi tanıması için bir `.addin` manifest dosyasına ihtiyacı vardır. Bu XML dosyası, Revit'e eklentinizin nerede olduğunu ve nasıl çalıştırılacağını söyler.

Aşağıda, bu proje için örnek bir **`v01.addin`** dosyası içeriği bulunmaktadır:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RevitAddIns>
  <AddIn Type="Command">
    <!-- EKLENTİNİN .DLL DOSYASININ TAM YOLU -->
    <Assembly>C:\ProgramData\Autodesk\Revit\Addins\2024\v01\v01.dll</Assembly>
    
    <!-- Yeni bir GUID oluşturmak için Visual Studio'da Tools > Create GUID kullanabilirsiniz. -->
    <AddInId>A1B3C5D7-E9F1-2345-6789-0123456789AB</AddInId>
    
    <!-- KOMUTU ÇALIŞTIRACAK SINIFIN TAM ADI -->
    <!-- Format: Namespace.ClassName -->
    <FullClassName>v01.ImportCommand</FullClassName>
    
    <!-- REVIT ARAYÜZÜNDE GÖRÜNECEK BUTON METNİ -->
    <Text>STA4CAD Aktar</Text>
    
    <!-- GELİŞTİRİCİ BİLGİLERİ -->
    <VendorId>KS</VendorId>
    <VendorDescription>Kerem Sarı, www.linkedin.com/in/keremsar</VendorDescription>
  </AddIn>
</RevitAddIns>
```

> **⚠️ ÖNEMLİ:** `<Assembly>` etiketindeki dosya yolunu, dosyaları kopyalayacağınız **gerçek konuma göre** güncellemeniz kritik öneme sahiptir.

**Adım 3: Gerekli Dosyaları Revit'e Kopyalama**

1.  Revit'in eklenti klasörüne gidin.
    * **Tüm kullanıcılar için:** `C:\ProgramData\Autodesk\Revit\Addins\[YIL]` (Önerilen)
    * **Sadece mevcut kullanıcı için:** `%APPDATA%\Autodesk\Revit\Addins\[YIL]`
2.  Bu klasörün içinde projeniz için yeni bir klasör oluşturun (Örn: `v01`).
3.  Aşağıdaki **üç dosyayı** da bu yeni oluşturduğunuz `v01` klasörünün içine kopyalayın:
    * `v01.dll` (C# projesinin derlenmiş hali)
    * `sta4cad-imp.jar` (Java projesinin derlenmiş hali)
    * `v01.addin` (Yukarıda hazırladığınız manifest dosyası)

#### Çalıştırma

1.  Kurulum adımlarını tamamladıktan sonra Revit'i (yeniden) başlatın.
2.  `Add-Ins` (Eklentiler) sekmesinde **"STA4CAD Aktar"** butonunu göreceksiniz.
3.  Butona tıklayın ve açılan pencereden işlemek istediğiniz `.ST4` dosyasını seçin. Eklenti, modeli otomatik olarak oluşturacaktır.
