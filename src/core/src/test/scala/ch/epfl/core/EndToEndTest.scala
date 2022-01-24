package ch.epfl.core

import ch.epfl.core.CustomMatchers._
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.{Constants, FileUtils}
import org.scalatest.concurrent.{Signaler, TimeLimitedTests}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import os.Path

import java.io.{ByteArrayOutputStream, StringReader}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class EndToEndTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with TimeLimitedTests {
  def timeLimit: Span = 30 seconds

  override val defaultTestSignaler: Signaler = (testThread: Thread) => testThread.stop()

  private val defaultIgnoredFiles = List(".DS_Store", ".gitkeep")
  private val endToEndResPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "endToEnd"
  private val pipeline1Path = endToEndResPath / "pipeline1_app_one_valid"
  private val pipeline2Path = endToEndResPath / "pipeline2_app_one_invalid_bindings"
  private val pipeline3Path = endToEndResPath / "pipeline3_app_one_app_two_valid"
  private val pipeline4Path = endToEndResPath / "pipeline4_app_one_app_two_invalid"

  private val inputPath = Constants.SVSHI_HOME_PATH / "input"
  private val appProtoFileName = "app_prototypical_structure.json"
  private val etsProjectFileName = "ets_proj.knxproj"

  private val runtimePath = SVSHI_SRC_FOLDER_PATH / "runtime_test"
  private val runtimeMainPath = runtimePath / "runtime_main.py"

  private val backupLibraryPath = SVSHI_SRC_FOLDER_PATH / "backup_library_during_test"
  private val backupGeneratedPath = SVSHI_SRC_FOLDER_PATH / "backup_generated_during_test"
  private val backupInputPath = SVSHI_SRC_FOLDER_PATH / "backup_input_during_test"

  override def beforeAll(): Unit = {
    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInputPath)) os.remove.all(backupInputPath)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.copy(APP_LIBRARY_FOLDER_PATH, backupLibraryPath)
    if (os.exists(GENERATED_FOLDER_PATH)) os.copy(GENERATED_FOLDER_PATH, backupGeneratedPath)
    if (os.exists(inputPath)) os.copy(inputPath, backupInputPath)
  }

  override def afterAll(): Unit = {
    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    if (os.exists(inputPath)) os.remove.all(inputPath)

    if (os.exists(backupLibraryPath)) os.copy(backupLibraryPath, APP_LIBRARY_FOLDER_PATH)
    if (os.exists(backupGeneratedPath)) os.copy(backupGeneratedPath, GENERATED_FOLDER_PATH)
    if (os.exists(backupInputPath)) os.copy(backupInputPath, inputPath)

    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInputPath)) os.remove.all(backupInputPath)
  }

  override def beforeEach(): Unit = {
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    if (os.exists(inputPath)) os.remove.all(inputPath)
    os.makeDir.all(inputPath)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    if (os.exists(runtimePath)) os.remove.all(runtimePath)
    os.makeDir.all(runtimePath)

    Main.setSystemExit(MockSystemExit)
  }

  override def afterEach(): Unit = {
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    if (os.exists(inputPath)) os.remove.all(inputPath)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    if (os.exists(runtimeMainPath)) os.remove(runtimeMainPath)
  }

  "getVersion" should "print the CLI version" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("version"))) match {
        case Failure(exception) => fail(exception)
        case Success(_) =>
          out.toString().contains("svshi v") shouldBe true
      }
    }
  }

  // Pipeline 1 - valid app with no other installed apps
  "generateApp" should "delete the json prototypical structure after generating the app - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val appName = "test_app_one"

    // Test
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))
    }

    val newAppPath = GENERATED_FOLDER_PATH / appName
    os.exists(pathToProto) shouldBe false
    out.toString.trim should include(s"The app '$appName' has been successfully created!")
  }

  "generateApp" should "generate the correct app in generated folder when called on a valid json prototypical structure - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val appName = "test_app_one"

    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))
    }

    val expectedAppPath = pipeline1Path / "test_app_one_valid_fresh_generated"

    val newAppPath = GENERATED_FOLDER_PATH / appName
    os.exists(newAppPath) shouldBe true

    // Compare everything to the expected app

    out.toString.trim should (include(s"The app '$appName' has been successfully created!"))

    os.exists(newAppPath / appProtoFileName) shouldBe true
    newAppPath / appProtoFileName should beAFile
    newAppPath / appProtoFileName should haveSameContentAsIgnoringBlanks(pipeline1Path / protoFileName)

    compareFolders(newAppPath, expectedAppPath, defaultIgnoredFiles)
  }

  "generateApp" should "fail when the name does not follow the rules - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val appName = "test_app_one42"

    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateApp", "-n", appName, "-d", pathToProto.toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                """ERROR: The app name has to contain only lowercase letters and underscores"""
              )
              val newAppPath = GENERATED_FOLDER_PATH / appName
              os.exists(newAppPath) shouldBe false
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }

  }

  "generateApp" should "fail when no name is provided for the new app - pipeline 1" in {
    // Prepare everything for the test
    val protoFileName = "test_app_one_proto.json"
    val pathToProto = inputPath / protoFileName
    os.copy(pipeline1Path / protoFileName, pathToProto)

    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateApp", "-d", pathToProto.toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                """ERROR: The app name has to be provided to generate a new app"""
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
  }

  "generateApp" should "fail when no prototypical file is passed - pipeline 1" in {

    val appName = "test_app_one"

    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateApp", "-n", appName))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                """ERROR: The devices prototypical structure JSON file has to be provided to generate a new app"""
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
  }

  "generateApp" should "fail when no prototypical file and no name are passed - pipeline 1" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateApp"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                """ERROR: The devices prototypical structure JSON file and the app name have to be provided to generate a new app"""
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
  }

  "generateApp" should "fail when the prototypical file name is not absolute" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateApp", "-n", "test", "-d", "proto.json"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The devices prototypical structure JSON file name has to be absolute"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
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
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("generateBindings", "-f", (inputPath / etsProjectFileName).toString))
    }

    os.exists(GENERATED_FOLDER_PATH / "physical_structure.json") shouldBe true
    GENERATED_FOLDER_PATH / "physical_structure.json" should beAFile()
    GENERATED_FOLDER_PATH / "physical_structure.json" should haveSameContentAsIgnoringBlanks(pipeline1Path / "physical_structure.json")

    os.exists(GENERATED_FOLDER_PATH / "apps_bindings.json") shouldBe true
    GENERATED_FOLDER_PATH / "apps_bindings.json" should beAFile()
    GENERATED_FOLDER_PATH / "apps_bindings.json" should haveSameContentAsIgnoringBlanks(pipeline1Path / "apps_bindings.json")

    out.toString.trim should (include("""The bindings have been successfully created!"""))
  }

  "generateBindings" should "fail when the ETS project file name is not absolute" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateBindings", "-f", "ets.knxproj"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The ETS project file name has to be absolute"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
  }

  "compile" should "install the app one when it is valid and verified and show output warning and success messages" in {
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
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))
    }

    out.toString.trim should (include("The apps have been successfully compiled and verified!") and
      include(
        "WARNING: Proto device name = binary_sensor_instance_name, type = binary; physical device address = (1,1,7), commObject = Telegr. counter value 2 bytes - Telegr. switch - Eingang A - Input A, physicalId = 1542297768: one KNXDatatype is UnknownDPT, attention required!"
      ) and
      include("info: Confirmed over all paths."))
    val newAppPath = APP_LIBRARY_FOLDER_PATH / appName
    os.exists(newAppPath) shouldBe true
    os.isDir(newAppPath) shouldBe true

    val expectedLibraryPath = pipeline1Path / "expected_library"

    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
  }

  "compile" should "fail when the ETS project file name is not absolute" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile", "-f", "ets.knxproj"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The ETS project file name has to be absolute"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The generation should have failed!")
      }
    }
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
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString.trim should (include(
                "ERROR: Proto device name = binary_sensor_instance_name, type = binary; physical device address = (1,1,10), commObject = Send - CO2 value, physicalId = -2092645687: KNXDatatype 'DPT-1' is incompatible with KNXDatatype 'DPT-9'!"
              ) and
                include("ERROR: Compilation/verification failed, see messages above"))
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
  }

  // Pipeline 3 - 2 valid apps: app one and then app two

  "run" should "execute the runtime module" in {
    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    val pathToExpectedFile = inputPath / "ok.txt"
    os.copy(endToEndResPath / "runtime_main.py", runtimeMainPath)

    Svshi.setRuntimeModulePath(runtimePath)
    Svshi.setRuntimeModule(s"runtime_test.runtime_main")

    val out = new ByteArrayOutputStream()
    val err = new ByteArrayOutputStream()
    Console.withOut(out) {
      Console.withErr(err) {
        Main.main(Array("run", "-a", "192.0.0.1:42"))
      }
    }

    os.exists(pathToExpectedFile) shouldBe true
    out.toString.trim should (include("this a line of text printed by the runtime module on stdout") and
      include("this is a line of text printed by runtime module on stdout after 3 sec") and
      include("this a line of text printed by the runtime module on stderr"))
    err.toString.trim shouldEqual ""
  }

  "run" should "fail if no KNX address and port have been provided" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("run"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The KNX address and port need to be specified to run the apps"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The execution should have failed!")
      }
    }
  }

  "run" should "fail if the KNX address has the wrong format" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("run", "-a", "1234.2.3:10"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The KNX address and port need to have the format 'address:port' where address is a valid IPv4 address and port a valid port"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The execution should have failed!")
      }
    }
  }

  "run" should "fail if the KNX port has the wrong format" in {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("run", "-a", "192.2.3.1:abc"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                "ERROR: The KNX address and port need to have the format 'address:port' where address is a valid IPv4 address and port a valid port"
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The execution should have failed!")
      }
    }
  }

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
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(Array("generateBindings", "-f", (inputPath / etsProjectFileName).toString))
    }

    // Check
    out.toString.trim should include("The bindings have been successfully created!")
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
    (GENERATED_FOLDER_PATH / "apps_bindings.json") should haveSameContentAsIgnoringBlanks(expectedGenerated / "apps_bindings.json")

  }

  "generateBindings" should "print an error if the ETS project path pointed to a non-existing file" in {
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
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateBindings", "-f", (inputPath / "non-existing_ets_project.knxproj").toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              // Check
              out.toString.trim should include("ERROR: The ETS Project file does not exist!")
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The bindings generation should have failed!")
      }
    }

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

  "generateBindings" should "fail with an error if no project ETS file is passed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"

    // Copy files for app one
    os.copy(pipeline3Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appOneName)

    // Generate the bindings
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("generateBindings"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                s"""ERROR: The ETS project file needs to be specified to compile or to generate the bindings"""
              )
            }
            case e: Exception => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
          }
        case Success(_) => fail("The bindings generation should have failed!")
      }
    }
  }

  "compile" should "fail with an error if no project ETS file is passed" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"

    // Copy files for app one
    os.copy(pipeline3Path / "test_app_one_valid_filled", GENERATED_FOLDER_PATH / appOneName)

    // Generate the bindings
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile"))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString should include(
                s"""ERROR: The ETS project file needs to be specified to compile or to generate the bindings"""
              )
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

  "removeApp" should "remove one app and keep the other installed with correct bindings, answer == y" in {
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

  "removeApp" should "not print info and success messages of the bindings, answer == y" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    val out = new ByteArrayOutputStream()
    Console.withIn(in) {
      Console.withOut(out) {
        Main.main(Array("removeApp", "-n", appTwoName))
      }
    }
    // Check
    out.toString.trim shouldNot include("The bindings have been successfully created!")
    out.toString.trim shouldNot include("Generating the bindings...")
    val expectedLibrary = pipeline3Path / "expected_library_one"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "removeApp" should "do nothing and keep installed apps as is if answer == n" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "n\n"
    val in = new StringReader(inputStr)
    val out = new ByteArrayOutputStream()
    Console.withIn(in) {
      Console.withOut(out) {
        Main.main(Array("removeApp", "-n", appTwoName))
      }
    }
    // Check
    out.toString.trim should include("Exiting...")
    val expectedLibrary = pipeline3Path / "expected_app_library_one_two"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "removeApp" should "remove one app and keep the other installed with correct bindings, answer == yes" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Main.main(Array("removeApp", "-n", appTwoName))
    }
    // Check
    val expectedLibrary = pipeline3Path / "expected_library_one"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
  }

  "removeApp" should "remove one app and preserve the content of the Generated folder" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Populate generated folder
    os.copy.into(pipeline3Path / "test_app_two_proto.json", GENERATED_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    val out = new ByteArrayOutputStream()
    Console.withIn(in) {
      Console.withOut(out) {
        Main.main(Array("removeApp", "-n", appTwoName))
      }
    }
    // Check
    out.toString.trim should include(s"The app '$appTwoName' has been successfully removed!")
    val expectedLibrary = pipeline3Path / "expected_library_one"
    compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibrary, defaultIgnoredFiles)
    os.exists(GENERATED_FOLDER_PATH / "test_app_two_proto.json") shouldBe true
    GENERATED_FOLDER_PATH / "test_app_two_proto.json" should beAFile()
    GENERATED_FOLDER_PATH / "test_app_two_proto.json" should haveSameContentAsIgnoringBlanks(pipeline3Path / "test_app_two_proto.json")
  }

  "removeApp" should "remove temp generated and temps library folders" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Populate generated folder
    os.copy.into(pipeline3Path / "test_app_two_proto.json", GENERATED_FOLDER_PATH)

    // Remove app two
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Main.main(Array("removeApp", "-n", appTwoName))
    }
    // Check
    os.exists(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH) shouldBe false
    os.exists(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH) shouldBe false
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
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withOut(out) {
      Console.withIn(in) {
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

  }

  "removeApp" should "write an error message when no name is passed" in {
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
    val inputStr = "y\n"
    val in = new StringReader(inputStr)
    Console.withOut(out) {
      Console.withIn(in) {
        Try(Main.main(Array("removeApp"))) match {
          case Failure(exception) =>
            exception match {
              case MockSystemExitException(errorCode) => out.toString.trim should (include("""ERROR: The app name has to be provided to remove an app"""))
              case e: Exception                       => fail(s"Unwanted exception occurred! exception = ${e.getLocalizedMessage}")
            }
          case Success(_) => fail("The removing should have failed!")
        }
      }
    }

  }

  "removeApp" should "remove all apps when called with --all answer == y" in {
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

  "removeApp" should "remove all apps when called with --all answer == yes" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_app_library_one_two", APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
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
    os.copy(pipeline4Path / etsProjectFileName, inputPath / etsProjectFileName)

    // Install app one
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.copy(pipeline3Path / "expected_library_one", APP_LIBRARY_FOLDER_PATH)

    // Prepare for app two
    os.copy.into(pipeline4Path / "physical_structure.json", GENERATED_FOLDER_PATH)
    os.copy(pipeline4Path / "apps_bindings_filled_one_two.json", GENERATED_FOLDER_PATH / "apps_bindings.json")
    os.copy(pipeline4Path / "test_app_two_invalid_filled_violates", GENERATED_FOLDER_PATH / appTwoName)

    // Compile app two
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Try(Main.main(Array("compile", "-f", (inputPath / etsProjectFileName).toString))) match {
        case Failure(exception) =>
          exception match {
            case MockSystemExitException(errorCode) => {
              out.toString.trim should (include(s"""error: false when calling test_app_two_iteration"""))
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
  }

  "removeApp" should "fail to remove an app when the remaining library cannot compile (invalid bindings), print an error and restore the library as before" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    val expectedLibraryPath = pipeline4Path / "expected_app_library_one_two_compile_error_invalid_bindings"
    os.copy(expectedLibraryPath, APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
    val in = new StringReader(inputStr)
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Console.withIn(in) {
        Try(Main.main(Array("removeApp", "-n", appTwoName))) match {
          case Failure(exception) =>
            exception match {
              case MockSystemExitException(errorCode) => {
                out.toString.trim should (include(s"""ERROR: Removing the application '$appTwoName' causes the verification of the remaining applications to fail."""))
                out.toString.trim should (include(s"""The app '$appTwoName' has not been removed."""))
                compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
              }
              case e: Exception => fail(s"Unwanted exception occurred! exception = ${e}")
            }
          case Success(_) => fail("The compilation should have failed!")
        }
      }
    }
  }

  "removeApp" should "fail to remove an app when the remaining library cannot verify (violated invariant), print an error and restore the library as before" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    val expectedLibraryPath = pipeline4Path / "expected_app_library_one_two_verification_error"
    os.copy(expectedLibraryPath, APP_LIBRARY_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
    val in = new StringReader(inputStr)
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Console.withIn(in) {
        Try(Main.main(Array("removeApp", "-n", appTwoName))) match {
          case Failure(exception) =>
            exception match {
              case MockSystemExitException(errorCode) => {
                out.toString.trim should (include(
                  s"""ERROR: Removing the application '$appTwoName' causes the verification of the remaining applications to fail. Please see trace above for more information. The app '$appTwoName' has not been removed."""
                ))
                out.toString.trim should (include("error: false when calling test_app_one_iteration("))
                compareFolders(APP_LIBRARY_FOLDER_PATH, expectedLibraryPath, ignoredFileNames = defaultIgnoredFiles)
              }
              case e: Exception => fail(s"Unwanted exception occurred! exception = ${e}")
            }
          case Success(_) => fail("The compilation should have failed!")
        }
      }
    }
  }

  "removeApp" should "preserve generated folder content when failing" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    val expectedLibraryPath = pipeline4Path / "expected_app_library_one_two_compile_error_invalid_bindings"
    os.copy(expectedLibraryPath, APP_LIBRARY_FOLDER_PATH)

    // Populate generated folder
    os.copy.into(pipeline3Path / "test_app_two_proto.json", GENERATED_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Try(Main.main(Array("removeApp", "-n", appTwoName)))
    }

    os.exists(GENERATED_FOLDER_PATH / "test_app_two_proto.json") shouldBe true
    GENERATED_FOLDER_PATH / "test_app_two_proto.json" should beAFile()
    GENERATED_FOLDER_PATH / "test_app_two_proto.json" should haveSameContentAsIgnoringBlanks(pipeline3Path / "test_app_two_proto.json")
  }

  "removeApp" should "remove temps library and temp generated folders when failing" in {
    // Prepare everything for the test
    val appOneName = "test_app_one"
    val appTwoName = "test_app_two"

    // Install app one and app two
    os.remove.all(APP_LIBRARY_FOLDER_PATH)
    val expectedLibraryPath = pipeline4Path / "expected_app_library_one_two_compile_error_invalid_bindings"
    os.copy(expectedLibraryPath, APP_LIBRARY_FOLDER_PATH)

    // Populate generated folder
    os.copy.into(pipeline3Path / "test_app_two_proto.json", GENERATED_FOLDER_PATH)

    // Remove app two
    val inputStr = "yes\n"
    val in = new StringReader(inputStr)
    Console.withIn(in) {
      Try(Main.main(Array("removeApp", "-n", appTwoName)))
    }

    os.exists(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH) shouldBe false
    os.exists(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH) shouldBe false

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
        f should haveSameContentAsIgnoringBlanks(fIn2)
      }
    }
  }

  object MockSystemExit extends SystemExit {
    override def exit(errorCode: Int): Unit = throw new MockSystemExitException(errorCode)
  }
  case class MockSystemExitException(errorCode: Int) extends Exception
}