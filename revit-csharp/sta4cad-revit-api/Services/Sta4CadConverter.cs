using Newtonsoft.Json;
using System;
using System.Diagnostics;
using System.IO;
using Autodesk.Revit.UI; 
using sta4cad_revit_api.Data; 

namespace v01.Services
{
    public class Sta4CadConverter
    {
        public ProjectData Convert(string st4FilePath)
        {
            string addinPath = Path.GetDirectoryName(typeof(ImportCommand).Assembly.Location);
            string jarPath = Path.Combine(addinPath, "sta4cad-imp.jar");

            if (!File.Exists(jarPath))
            {
                string message = $".jar dosyası bulunamadı. Lütfen 'sta4cad-imp.jar' dosyasının şu konumda olduğundan emin olun:\n{addinPath}";
                TaskDialog.Show("Hata", message);
                return null;
            }

            string jsonFilePath = Path.Combine(Path.GetTempPath(), Guid.NewGuid().ToString() + ".json");

            try
            {
                //.jar dosyasını arkaplanda çalıştırır
                ExecuteJarProcess(jarPath, st4FilePath, jsonFilePath);

                if (!File.Exists(jsonFilePath))
                {
                    TaskDialog.Show("İşlem Hatası", "Java .jar dosyası çalıştırıldı ancak beklenen JSON çıktısını oluşturmadı. Java konsolunu kontrol edin.");
                    return null;
                }

                string jsonContent = File.ReadAllText(jsonFilePath);
                ProjectData projectData = JsonConvert.DeserializeObject<ProjectData>(jsonContent);

                if (projectData == null)
                {
                    TaskDialog.Show("JSON Hatası", "Oluşturulan JSON dosyası ayrıştırılamadı. Format hatalı olabilir.");
                    return null;
                }

                return projectData;
            }
            catch (Exception ex)
            {
                string message = "Veri dönüştürme sırasında bir hata oluştu.\n" +
                                 "Lütfen Java'nın sisteminizde yüklü ve PATH ortam değişkenine ekli olduğundan emin olun.\n\n" +
                                 "Detay: " + ex.Message;
                TaskDialog.Show("Proses Hatası", message);
                return null;
            }
            finally
            {
                if (File.Exists(jsonFilePath))
                {
                    try { File.Delete(jsonFilePath); }
                    catch 
                    {
                        
                    }
                }
            }
        }

        private void ExecuteJarProcess(string jarPath, string inputFile, string outputFile)
        {
            ProcessStartInfo startInfo = new ProcessStartInfo("java.exe")
            {
                Arguments = $"-jar \"{jarPath}\" \"{inputFile}\" \"{outputFile}\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            using (Process process = Process.Start(startInfo))
            {
                string output = process.StandardOutput.ReadToEnd();
                string error = process.StandardError.ReadToEnd();

                process.WaitForExit();

                Debug.WriteLine("Java process output:\n" + output);
                if (!string.IsNullOrEmpty(error))
                {
                    Debug.WriteLine("Java process error:\n" + error);
                }

                if (process.ExitCode != 0)
                {
                    throw new Exception($"Java işlemi {process.ExitCode} hata koduyla sonlandı.\n\nHata Mesajı:\n{error}\n\nÇıktı Mesajı:\n{output}");
                }
            }
        }
    }
}
