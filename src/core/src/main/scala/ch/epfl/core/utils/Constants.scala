package ch.epfl.core.utils

import os.Path

object Constants {
  lazy private val DEFAULT_SVSHI_HOME =
    if (sys.env.contains("HOME")) s"${sys.env("HOME")}/svshi" else if (sys.env.contains("HOMEPATH")) s"${sys.env("HOME")}/svshi" else ""
  val SVSHI_HOME: String = if (sys.env.contains("SVSHI_HOME")) sys.env("SVSHI_HOME") else DEFAULT_SVSHI_HOME

  lazy val SVSHI_HOME_PATH = os.Path(SVSHI_HOME)
  val SVSHI_SRC_FOLDER = s"$SVSHI_HOME/src"
  lazy val SVSHI_SRC_FOLDER_PATH = os.Path(SVSHI_SRC_FOLDER)
  val ASSIGNMENTS_DIRECTORY_NAME = s"$SVSHI_HOME/assignments"
  val PHYSICAL_STRUCTURE_JSON_FILE_NAME = "physical_structure.json"
  val APP_PROTO_BINDINGS_JSON_FILE_NAME = "apps_bindings.json"
  val APP_PYTHON_ADDR_BINDINGS_FILE_NAME = "addresses.json"
  val GROUP_ADDRESSES_LIST_FILE_NAME = "group_addresses.json"
  val APP_PROTO_STRUCT_FILE_NAME = "app_prototypical_structure.json"
  val GENERATED_FOLDER_PATH_STRING = s"$SVSHI_HOME/generated"
  val GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH_STRING = s"$SVSHI_SRC_FOLDER/temp_generated"
  lazy val GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH = os.Path(GENERATED_TEMP_FOLDER_DURING_REMOVING_PATH_STRING)
  val APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH_STRING = s"$SVSHI_SRC_FOLDER/temp_app_library"
  lazy val APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH = os.Path(APP_LIBRARY_TEMP_FOLDER_DURING_REMOVING_PATH_STRING)
  val APP_LIBRARY_FOLDER_PATH_STRING = s"$SVSHI_SRC_FOLDER/app_library"
  lazy val GENERATED_FOLDER_PATH: Path = os.Path(GENERATED_FOLDER_PATH_STRING)
  lazy val APP_LIBRARY_FOLDER_PATH: Path = os.Path(APP_LIBRARY_FOLDER_PATH_STRING)
  val GENERATED_VERIFICATION_FILE_NAME = "verification_file.py"
  lazy val GENERATED_VERIFICATION_FILE_PATH: Path = os.Path(s"$SVSHI_SRC_FOLDER/verification/$GENERATED_VERIFICATION_FILE_NAME")
  val GENERATED_RUNTIME_FILE_NAME = "runtime_file.py"
  lazy val GENERATED_RUNTIME_FILE_PATH: Path = os.Path(s"$SVSHI_SRC_FOLDER/verification/$GENERATED_RUNTIME_FILE_NAME")
  val GENERATED_CONDITIONS_FILE_NAME = "conditions.py"
  lazy val GENERATED_CONDITIONS_FILE_PATH: Path = os.Path(s"$SVSHI_SRC_FOLDER/verification/$GENERATED_CONDITIONS_FILE_NAME")
  val VERIFICATION_PYTHON_MODULE = "verification.main"
  val APP_GENERATOR_PYTHON_MODULE = "generator.main"
  val RUNTIME_PYTHON_MODULE = "runtime.main"
  lazy val RUNTIME_PYTHON_MODULE_PATH: Path = os.Path(s"$SVSHI_SRC_FOLDER/runtime")
  val CROSSHAIR_TIMEOUT_SECONDS = 600
  val DELETED_APPS_FOLDER_PATH_STRING = s"$SVSHI_HOME/deletedApps"
  lazy val DELETED_APPS_FOLDER_PATH = os.Path(DELETED_APPS_FOLDER_PATH_STRING)
}
