package ch.epfl.core

import ch.epfl.core.CustomMatchers._
import ch.epfl.core.utils.Constants.{APP_LIBRARY_FOLDER_PATH, GENERATED_FOLDER_PATH}
import ch.epfl.core.utils.{Constants, FileUtils}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import os.Path

import java.io.{ByteArrayOutputStream, StringReader}
import scala.util.{Failure, Success, Try}

class EndToEndTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  private val defaultIgnoredFiles = List(".DS_Store", ".gitkeep")
  private val endToEndResPath = Constants.SVSHI_FOLDER_PATH / "core" / "res" / "endToEnd"
  private val pipeline1Path = endToEndResPath / "pipeline1_app_one_valid"
  private val pipeline2Path = endToEndResPath / "pipeline2_app_one_invalid_bindings"
  private val pipeline3Path = endToEndResPath / "pipeline3_app_one_app_two_valid"
  private val pipeline4Path = endToEndResPath / "pipeline4_app_one_app_two_invalid"

  private val inputPath = Constants.SVSHI_HOME_PATH / "input"
  private val appProtoFileName = "app_prototypical_structure.json"
  private val etsProjectFileName = "ets_proj.knxproj"

  override def beforeEach(): Unit = {
    os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    os.remove.all(inputPath)
    os.makeDir.all(inputPath)

    os.remove.all(Constants.APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(Constants.APP_LIBRARY_FOLDER_PATH)
    Main.setSystemExit(MockSystemExit)
  }

  override def afterEach(): Unit = {
    os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    os.remove.all(inputPath)
    os.makeDir.all(inputPath)

    os.remove.all(Constants.APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(Constants.APP_LIBRARY_FOLDER_PATH)
  }

  // Pipeline 1 - valid app with no other installed apps
  "generateApp" should "delete the json prototypical structure after generating the app - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val appName = "test_app_one"

    // Test
    Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))

    val newAppPath = GENERATED_FOLDER_PATH / appName
    os.exists(pathToProto) shouldBe false
  }

  "generateApp" should "generate the correct app in generated folder when called on a valid json prototypical structure - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val appName = "test_app_one"

    Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))

    val expectedAppPath = pipeline1Path / "test_app_one_valid_fresh_generated"

    val newAppPath = GENERATED_FOLDER_PATH / appName
    os.exists(newAppPath) shouldBe true

    // Compare everything to the expected app

    os.exists(newAppPath / appProtoFileName) shouldBe true
    newAppPath / appProtoFileName should beAFile
    newAppPath / appProtoFileName should haveSameContentAs(pipeline1Path / protoFileName)

    compareFolders(newAppPath, expectedAppPath, defaultIgnoredFiles)
  }

  "generateBindings" should "generate the correct bindings and physical_structure.json with only one app in generated and none installed - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)
    os.copy(pipeline1Path / etsProjectFileName, inputPath / etsProjectFileName)
    val appName = "test_app_one"

    // Generate the app
    Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))

    // Generate the bindings
    Main.main(Array("generateBindings", "-f", (inputPath / etsProjectFileName).toString))

    os.exists(GENERATED_FOLDER_PATH / "physical_structure.json") shouldBe true
    GENERATED_FOLDER_PATH / "physical_structure.json" should beAFile()
    GENERATED_FOLDER_PATH / "physical_structure.json" should haveSameContentAs(pipeline1Path / "physical_structure.json")

    os.exists(GENERATED_FOLDER_PATH / "apps_bindings.json") shouldBe true
    GENERATED_FOLDER_PATH / "apps_bindings.json" should beAFile()
    GENERATED_FOLDER_PATH / "apps_bindings.json" should haveSameContentAs(pipeline1Path / "apps_bindings.json")
  }

  "compile" should "install the app one when it is valid and verified" in {
    // Prepare everything for the test
    val appName = "test_app_one"
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appName)
    os.copy.into(pipeline1Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline1Path / "apps_bindings_filled.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline1Path / protoFileName, pathToProto)
    os.copy(pipeline1Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Compile the app
    Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))
    val newAppPath = APP_LIBRARY_FOLDER_PATH / appName
    os.exists(newAppPath) shouldBe true
    os.isDir(newAppPath) shouldBe true

    val expectedLibraryPath = pipeline1Path / "expected_library"

    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
  }

  // Pipeline 2 - valid app with no other installed apps but invalid bindings
  "compile" should "not install the app one when bindings are invalid" in {
    // Prepare everything for the test
    val appName = "test_app_one"
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline2Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appName)
    os.copy.into(pipeline2Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline2Path / "apps_bindings_filled.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline2Path / protoFileName, pathToProto)
    os.copy(pipeline2Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Compile the app
    Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
      case Failure(exception) =>
        exception match {
          case MockSystemExitException(errorCode) => {
            val newAppPath = APP_LIBRARY_FOLDER_PATH / appName
            os.exists(newAppPath) shouldBe false

            // Empty library
            val expectedLibraryPath = pipeline2Path / "expected_library"
            compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
          }
          case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
        }
      case Success(_) => fail("The compilation should have failed!")
    }
  }

  // Pipeline 3 - 2 valid apps: app one and then app two
  "generateBindings" should "generate the bindings keeping the old ones if physical structure didn't change" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Copy files for app two
    os.copy(pipeline3Path / "test_app_two_fresh_generated", GENERATED_FOLDER_PATH / appTwoName)

    // Generate the bindings
    Main.main(Array("generateBindings", "-f", (inputPath / etsProjectFileName).toString))

    // Check
    val expectedGenerated = pipeline3Path / "expected_generated_app_two"
    compareFolders(GENERATED_FOLDER_PATH, expectedGenerated, defaultIgnoredFiles)
  }

  "generateBindings" should "generate the bindings with -1 everywhere if physical structure changed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)
    os.copy(pipeline3Path / "other_ets_project.knxproj", inputPath / "other_ets_project.knxproj")

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Copy files for app two
    os.copy(pipeline3Path / "test_app_two_fresh_generated", GENERATED_FOLDER_PATH / appTwoName)

    // Generate the bindings
    Main.main(Array("generateBindings", "-f", (inputPath / "other_ets_project.knxproj").toString))

    // Check
    val expectedGenerated = pipeline3Path / "expected_generated_app_two_other_knxproj"
    os.exists(GENERATED_FOLDER_PATH / "apps_bindings.json") shouldBe true
    GENERATED_FOLDER_PATH / "apps_bindings.json" should beAFile()
    (GENERATED_FOLDER_PATH / "apps_bindings.json") should haveSameContentAs(expectedGenerated / "apps_bindings.json")

  }

  "compile" should "install app two when both apps are valid, bindings are valid and no app violates invariants" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Prepare for app two
    os.copy.into(pipeline3Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline3Path / "apps_bindings_filled_one_two.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline3Path / "test_app_two_valid_filled", GENERATED_FOLDER_PATH / appTwoName)

    // Compile app two
    Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))

    // Check
    val expectedLibrary = pipeline3Path / "expected_app_library_one_two"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "compile" should "fail with an error message when compiling a new app with bindings that are not compatible 1" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Prepare for app two
    os.copy.into(pipeline3Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline3Path / "apps_bindings_filled_one_two_invalid_1.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline3Path / "test_app_two_valid_filled", GENERATED_FOLDER_PATH / appTwoName)

    // Compile app two
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                """ERROR: The bindings are not compatible with the apps you want to install! Please run generateBindings again and fill them before compiling again."""
              )
              val newAppPath = APP_LIBRARY_FOLDER_PATH / appTwoName
              os.exists(newAppPath) shouldBe false

              // Library with only app one
              val expectedLibraryPath = pipeline3Path / "expected_library_one"
              compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The compilation should have failed!")
      }
    }
  }

  "compile" should "fail with an error message when compiling a new app with same name as another installed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_one"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Prepare for app two
    os.copy.into(pipeline3Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline3Path / "apps_bindings_filled_one_two_invalid_1.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline3Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appTwoName)

    // Compile app two
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                s"""ERROR: An application with the name '$appTwoName' is already installed! You cannot install two apps with the same name!"""
              )

              // Library with only app one
              val expectedLibraryPath = pipeline3Path / "expected_library_one"
              compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The compilation should have failed!")
      }
    }
  }

  "generateBindings" should "fail with an error if an app with the same name as the one being installed is already installed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_one"
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Copy files for app two
    os.copy(pipeline3Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appTwoName)

    // Generate the bindings
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateBindings", "-f", (inputPath / etsProjectFileName).toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                s"""ERROR: An application with the name '$appTwoName' is already installed! You cannot install two apps with the same name!"""
              )
              os.exists(GENERATED_FOLDER_PATH / "apps_bindings.json") shouldBe false
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The bindings generation should have failed!")
      }
    }
  }

  "listApps" should "return app one and app two when both are installed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Check
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("listApps"))
    }
    out.toString.trim should (include("""The installed apps are: 'test_app_one', 'test_app_two'""") and include("Listing the apps..."))
  }

  "removeApp" should "remove one app and keep the other installed with correct bindings" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Main.main(Array("removeApp", "-n", appTwoName))
    }
    // Check
    val expectedLibrary = pipeline3Path / "expected_library_one"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "removeApp" should "remove everything when we remove the last app" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Main.main(Array("removeApp", "-n", appTwoName))
    }

    val inputStr2 = "y\n"
    val in2 = new StringReader(inputStr)
    Console.withIn(in2) {
      Main.main(Array("removeApp", "-n", appOneName))
    }
    // Check
    val expectedLibrary = pipeline3Path / "expected_empty_library"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "removeApp" should "write an error message when the given app name is not installed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy.into(pipeline3Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline3Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Check
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("removeApp", "-n", appTwoName))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => out.toString.trim should (include("""ERROR: The app 'test_app_two' is not installed!"""))
            case e: Exception                       => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The removing should have failed!")
      }
    }

  }

  "removeApp" should "remove all apps when called with --all" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Main.main(Array("removeApp", "--all"))
    }
    // Check
    val expectedLibrary = pipeline3Path / "expected_empty_library"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  // Pipeline 4 - 2 apps: app one valid and then app two that violates app one invariant
  "compile" should "not install app two when it can violate invariant of app 1" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"
    os.copy(pipeline4Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appOneName)
    os.copy.into(pipeline4Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline4Path / "apps_bindings_filled_one.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline4Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))

    // Prepare for app two
    os.copy.into(pipeline4Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline4Path / "apps_bindings_filled_one_two.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline4Path / "test_app_two_invalid_filled_violates", GENERATED_FOLDER_PATH / appTwoName)

    // Compile app two
    Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
      case Failure(exception) =>
        exception match {
          case MockSystemExitException(errorCode) => {
            val newAppPath = APP_LIBRARY_FOLDER_PATH / appTwoName
            os.exists(newAppPath) shouldBe false

            // Library with only app one
            val expectedLibraryPath = pipeline4Path / "expected_library_one"
            compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
          }
          case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
        }
      case Success(_) => fail("The compilation should have failed!")
    }
  }

  // Compare the two folder and assert that they contain the same files and that files are identical
  private def compareFolders(folder1: Path, folder2: Path, ignoredFileNames: List[String]): Unit = {
    os.isDir(folder1) shouldBe true
    os.isDir(folder2) shouldBe true

    // Compare content of the folder
    val folder1Content = FileUtils.recursiveListFiles(folder1).filterNot(p => ignoredFileNames.contains(p.segments.toList.last))
    val folder2Content = FileUtils.recursiveListFiles(folder2).filterNot(p => ignoredFileNames.contains(p.segments.toList.last))

    for (e <- folder1Content) {
      folder2Content.map(f => f.relativeTo(folder2)) should contain(e.relativeTo(folder1))
    }
    for (e <- folder2Content) {
      folder1Content.map(f => f.relativeTo(folder1)) should contain(e.relativeTo(folder2))
    }

    for (f <- folder1Content) {
      if (os.isFile(f)) {
        val fRel = f.relativeTo(folder1)
        val fIn2 = os.Path(fRel, folder2)
        fIn2 should beAFile()
        f should haveSameContentAs(fIn2)
      }
    }
  }

  object MockSystemExit extends SystemExit {
    override def exit(errorCode: Int): Unit = throw new MockSystemExitException(errorCode)
  }
  case class MockSystemExitException(errorCode: Int) extends Exception
}