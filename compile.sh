#!/usr/bin/env bash

# Javolution Extended Interactive Build Script for Linux / Unix

main_menu() {
	while true; do
		echo ""
		echo "Select an action to perform:"
		echo "[b]uild"
		echo "[c]lean"
		echo "[e]xit"
		read -r -p "Enter your choice: " action
		case "$action" in
			[bB]|[bB][uU][iI][lL][dD])
				build_menu
				;;
			[cC]|[cC][lL][eE][aA][nN])
				clean_target
				;;
			[eE]|[eE][xX][iI][tT])
				exit 0
				;;
			*)
				echo "Only the values: 'b' or 'build', 'c' or 'clean', 'e' or 'exit' are allowed!"
				;;
		esac
	done
}

ensure_jdk5() {
	if [ -x "./jdk1.5.0_22/bin/javac" ] && [ -x "./jdk1.5.0_22/bin/javadoc" ]; then
		javac_executable="$(pwd)/jdk1.5.0_22/bin/javac"
		javadoc_executable="$(pwd)/jdk1.5.0_22/bin/javadoc"
	elif [ -f "./lib/jdk-5u22-linux-x64.bin" ]; then
		echo "Extracting JDK 5 installer from lib/jdk-5u22-linux-x64.bin..."
		chmod +x ./lib/jdk-5u22-linux-x64.bin
		./lib/jdk-5u22-linux-x64.bin
		javac_executable="$(pwd)/jdk1.5.0_22/bin/javac"
		javadoc_executable="$(pwd)/jdk1.5.0_22/bin/javadoc"
	else
		javac_executable=""
		javadoc_executable=""
	fi
}

ensure_jdk6() {
	if [ -x "./jdk1.6.0_45/bin/javac" ] && [ -x "./jdk1.6.0_45/bin/javadoc" ]; then
		javac_executable="$(pwd)/jdk1.6.0_45/bin/javac"
		javadoc_executable="$(pwd)/jdk1.6.0_45/bin/javadoc"
	elif [ -f "./lib/jdk-6u45-linux-x64.bin" ]; then
		echo "Extracting JDK 6 installer from lib/jdk-6u45-linux-x64.bin..."
		chmod +x ./lib/jdk-6u45-linux-x64.bin
		./lib/jdk-6u45-linux-x64.bin
		javac_executable="$(pwd)/jdk1.6.0_45/bin/javac"
		javadoc_executable="$(pwd)/jdk1.6.0_45/bin/javadoc"
	else
		javac_executable=""
		javadoc_executable=""
	fi
}

build_menu() {
	while true; do
		echo ""
		echo "Which compiler to use when compiling?"
		echo "[j2me] (MIDP 2.0 / CLDC 1.1)"
		echo "[1.4]  (J2SE 1.4+)"
		echo "[1.5]  (J2SE 1.5+)"
		echo "[1.6]  (J2SE 1.6+)"
		echo "[e]xit"
		read -r -p "Enter your choice: " compiler
		case "$compiler" in
			j2me|1.4|1.5)
				ensure_jdk5
				compile_target "$compiler"
				break
				;;
			1.6)
				ensure_jdk6
				compile_target "$compiler"
				break
				;;
			[eE]|[eE][xX][iI][tT])
				echo "Back to main menu..."
				break
				;;
			*)
				echo "Only the values: 'j2me', '1.4', '1.5', '1.6', or 'e' are allowed!"
				;;
		esac
	done
}

compile_target() {
	local target="$1"
	if [ -z "$javac_executable" ] || [ ! -x "$javac_executable" ]; then
		echo "Error: Java compiler executable not found ($javac_executable)!"
		return 1
	fi
	if [ -z "$javadoc_executable" ] || [ ! -x "$javadoc_executable" ]; then
		echo "Error: Javadoc executable not found ($javadoc_executable)!"
		return 1
	fi
	echo "Call ant $target command..."
	ant -Djavac.executable="$javac_executable" -Djavadoc.executable="$javadoc_executable" "$target"
	local status=$?
	if [ $status -ne 0 ]; then
		echo "Ant compile command failed! Error code: $status"
	else
		echo "Ant compile command completed successfully."
	fi
	return $status
}

clean_target() {
	echo "Call ant clean command..."
	ant clean
	local status=$?
	if [ $status -ne 0 ]; then
		echo "Ant clean command failed! Error code: $status"
	else
		echo "Ant clean command completed successfully."
	fi
	return $status
}

main_menu
