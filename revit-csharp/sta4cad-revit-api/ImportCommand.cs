using Autodesk.Revit.Attributes;
using Autodesk.Revit.DB;
using Autodesk.Revit.UI;
using sta4cad_revit_api.Data;
using System;
using System.Windows.Forms;
using v01.Services; 

namespace v01
{
    [Transaction(TransactionMode.Manual)]
    public class ImportCommand : IExternalCommand
    {
        public Result Execute(ExternalCommandData commandData, ref string message, ElementSet elements)
        {
            UIApplication uiapp = commandData.Application;
            Document doc = uiapp.ActiveUIDocument.Document;

            //Kullanıcıya .ST4 dosyasını seçtirir
            string st4FilePath = ShowSelectSt4FileDialog();
            if (string.IsNullOrEmpty(st4FilePath))
            {
                return Result.Cancelled;
            }

            try
            {
                // STA4CAD verisini işler ve C# nesnesinelrine ayrıştır 
                var converter = new Sta4CadConverter();
                ProjectData projectData = converter.Convert(st4FilePath);

                if (projectData == null)
                {
                    return Result.Failed;
                }

                // Revit modeli olarak aktar
                using (TransactionGroup tg = new TransactionGroup(doc, "STA4CAD Modelini Aktar"))
                {
                    tg.Start();

                    var modelBuilder = new RevitModelBuilder(doc);
                    modelBuilder.Build(projectData);

                    tg.Assimilate();
                }

                ShowSuccessDialog();
                return Result.Succeeded;
            }
            catch (Exception ex)
            {
                message = "Model oluşturulurken beklenmedik bir hata oluştu: " + ex.Message;
                TaskDialog.Show("Kritik Hata", message + "\n\nDetaylar:\n" + ex.ToString());
                return Result.Failed;
            }
        }

        private string ShowSelectSt4FileDialog()
        {
            using (OpenFileDialog openFileDialog = new OpenFileDialog
            {
                Filter = "STA4CAD Dosyaları (*.st4)|*.st4|Tüm Dosyalar (*.*)|*.*",
                Title = "Lütfen REVIT'e Aktarılacak STA4CAD Modelinin .ST4 Dosyasını Seçin."
            })
            {
                return openFileDialog.ShowDialog() == DialogResult.OK ? openFileDialog.FileName : null;
            }
        }

        private void ShowSuccessDialog()
        {
            TaskDialog mainDialog = new TaskDialog("Başarılı")
            {
                MainInstruction = "STA4CAD modeli başarıyla Revit'e aktarıldı!",
                MainContent = "Geliştirici: Kerem Sarı"
            };
            mainDialog.Show();
        }
    }
}
