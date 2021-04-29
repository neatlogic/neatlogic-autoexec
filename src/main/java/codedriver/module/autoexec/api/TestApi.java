package codedriver.module.autoexec.api;

import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.util.ArgumentTokenizer;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "autoexec/test";
	}

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Description(desc = "test")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject result = new JSONObject();
//		String text = "我有一颗大土豆，刚出锅的大的土豆豆";
//		List<String> words = new ArrayList<>();
//		words.add("土豆");
//		SensitiveUtil.init(words);
//		List<FoundWord> wordList = SensitiveUtil.getFoundAllSensitive(text);

		String shell = "#!/bin/bash\n" +
				"jdkTargz=\"/opt/jdk-8u221-linux-x64.tar.gz\"\n" +
				"# 检查原先是否已配置java环境变量\n" +
				"checkExist(){\n" +
				" jdk1=$(grep -n \"export JAVA_HOME=.*\" /etc/profile | cut -f1 -d':')\n" +
				"    if [ -n \"$jdk1\" ];then\n" +
				"        echo \"JAVA_HOME已配置，删除内容\"\n" +
				"        sed -i \"${jdk1}d\" /etc/profile\n" +
				"rm -f /etc/profile\n" +
				"eval \"rm -f xxx j rm hmkjhkhkhkjhkjhkhkjhj\"\n" +
				"rm -f xxx\n" +
				"mv /usr/runoob/*\n" +
				"exec \"rm -f xxx\"\n" +
				"kill -9 123456 \n" +
				"halt -p\n" +
				"groupdel hnuser\n" +
				"groupmod -n linux linuxso\n" +
				"useradd caojh -u 544\n" +
				"userdel hnlinux\n" +
				"usermod -u 777 root\n" +
				"killall\n" +
				"umount -v /mnt/mymount/\n" +
				"sysctl -w net.ipv4.ip_forward=1\n" +
				"atrm -d 055\n" +
				"    fi\n" +
				" jdk2=$(grep -n \"export CLASSPATH=.*\\$JAVA_HOME.*\" /etc/profile | cut -f1 -d':')\n" +
				"    if [ -n \"$jdk2\" ];then\n" +
				"        echo \"CLASSPATH路径已配置，删除内容\"\n" +
				"        sed -i \"${jdk2}d\" /etc/profile\n" +
				"    fi\n" +
				" jdk3=$(grep -n \"export PATH=.*\\$JAVA_HOME.*\" /etc/profile | cut -f1 -d':')\n" +
				"    if [ -n \"$jdk3\" ];then\n" +
				"        echo \"PATH-JAVA路径已配置，删除内容\"\n" +
				"        sed -i \"${jdk3}d\" /etc/profile\n" +
				"    fi\n" +
				"}\n" +
				"# 查询是否有jdk.tar.gz\n" +
				"if [ -e $jdkTargz ];\n" +
				"then\n" +
				"\n" +
				"echo \"— — 存在jdk压缩包 — —\"\n" +
				" echo \"正在解压jdk压缩包...\"\n" +
				" tar -zxvf /opt/jdk-8u221-linux-x64.tar.gz -C /opt\n" +
				" if [ -e \"/opt/install/java\" ];then\n" +
				" echo \"存在该文件夹，删除...\"\n" +
				" rm -rf /opt/install/java\n" +
				" fi\n" +
				" echo \"---------------------------------\"\n" +
				" echo \"正在建立jdk文件路径...\"\n" +
				" echo \"---------------------------------\"\n" +
				" mkdir -p /opt/install/java/\n" +
				" mv /opt/jdk1.8.0_221 /opt/install/java/java8\n" +
				" # 检查配置信息\n" +
				" checkExist \n" +
				" echo \"---------------------------------\"\n" +
				" echo \"正在配置jdk环境...\"\n" +
				" sed -i '$a export JAVA_HOME=/opt/install/java/java8' /etc/profile\n" +
				" sed -i '$a export CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar' /etc/profile\n" +
				" sed -i '$a export PATH=$PATH:$JAVA_HOME/bin' /etc/profile\n" +
				" echo \"---------------------------------\"\n" +
				" echo \"JAVA环境配置已完成...\"\n" +
				" echo \"---------------------------------\"\n" +
				"  echo \"正在重新加载配置文件...\"\n" +
				"  echo \"---------------------------------\"\n" +
				"  source /etc/profile\n" +
				"  echo \"配置版本信息如下：\"\n" +
				"  java -version\n" +
				"else\n" +
				" echo \"未检测到安装包，请将安装包放到/opt目录下\"\n" +
				"fi";

		String shell2 = "#!/bin/bash\n" +
				"/etc/profile\n" +
				"tomcatPath=\"/usr/local/tomcat9\"\n" +
				"binPath=\"$tomcatPath/bin\"\n" +
				"echo \"[info][$(date)]正在监控tomcat，路径：$tomcatPath\"\n" +
				"pid=`ps -ef | grep tomcat | grep -w $tomcatPath | grep -v 'grep' | awk '{print $2}'`\n" +
				"if [-n \"pid\"]; then\n" +
				"echo \"[info][$(date)]tomcat进程为：$pid\"\n" +
				"echo \"[info][$(date)]tomcat已经启动，准备使用shutdown命令关闭\"\n" +
				"$binPath\"/shutdown.sh\"\n" +
				"sleep 2\n" +
				"pid=`ps -ef | grep tomcat | grep -w $tomcatPath | grep -v 'grep' | awk '{print $2}'`\n" +
				"if [-n \"$pid\"]; then\n" +
				"echo \"[info][$(date)]使用shutdown关闭失败，准备kill进程\"\n" +
				"kill -9 $pid\n" +
				"echo \"[info][$(date)]kill进程完毕\"\n" +
				"sleep 1\n" +
				"else\n" +
				"echo \"[info][$(date)]使用shutdown关闭成功\"\n" +
				"fi\n" +
				"else\n" +
				"echo \"[info][$(date)]tomcat未启动\"\n" +
				"fi\n" +
				"echo \"[info][$(date)]准备启动tomcat\"\n" +
				"$binPath\"/startup.sh\"\n" +
				"echo 'tomcat启动成功'";

		String perl = "#!/usr/bin/perl\n" +
				"\n" +
				"use strict;\n" +
				"use warnings;\n" +
				"use Getopt::Std;\n" +
				"\n" +
				"sub show_help {\n" +
				"  print \"Useage:\\n\";\n" +
				"  print \"newp -aAnsl\\n\";\n" +
				"  print \"-a\\t\\t the password contains lower case letters(a-z)\\n\";\n" +
				"  print \"-A\\t\\t the password contains upper case letters(A-Z)\\n\";\n" +
				"  print \"-n\\t\\t the password contains numerical character(0-9)\\n\";\n" +
				"  print \"-s\\t\\t the password contains special symbols\\n\";\n" +
				"  print \"-u\\t\\t the password contains only unique characters\\n\";\n" +
				"  print \"-l length\\t set the password length(default: 6)\\n\";\n" +
				"\n" +
				"  exit 0;\n" +
				"}\n" +
				"\n" +
				"sub show_version {\n" +
				"  print \"Version: 0.2.1 Changed the default option: -l 9 -Ana. 2016-4-15\\n\";\n" +
				"\n" +
				"  exit 0;\n" +
				"}\n" +
				"\n" +
				"### main program\n" +
				"\n" +
				"use vars qw($opt_a $opt_A $opt_h $opt_l $opt_n $opt_s $opt_u $opt_v);\n" +
				"getopts('aAhl:nsuv');\n" +
				"\n" +
				"&show_version if $opt_v;\n" +
				"&show_help if $opt_h;\n" +
				"\n" +
				"my $len = $opt_l || 9;  # default length 9\n" +
				"my $opt_cnt = 0;\n" +
				"my @rand_str = ();\n" +
				"\n" +
				"# store all the characters\n" +
				"my @num = qw(0 1 2 3 4 5 6 7 8 9);\n" +
				"my @ABC = qw(A B C D E F G H I J K L M N O P Q R S T U V W X Y Z);\n" +
				"my @abc = qw(a b c d e f g h i j k l m n o p q r s t u v w x y z);\n" +
				"# my @sym = qw(! \" $ % & ' * + - . / : ; < = > ? @ [ \\ ] ^ _ ` { | } ~);\n" +
				"my @sym = qw(! $ % & * + - . / : ; < = > ? @ [ ] ^ _ ` { | } ~); # no \" ' \\\n" +
				"unshift (@sym, '(', ')', '#', ','); # to prevent perl's complains or warnings.\n" +
				"my @all_sym = (@num, @ABC, @abc, @sym);\n" +
				"my @ch_src = ();\n" +
				"\n" +
				"if ((!$opt_a) && (!$opt_A) && (!$opt_n) && (!$opt_s)) {\n" +
				"  $opt_a++;\n" +
				"  $opt_A++;\n" +
				"  $opt_n++;\n" +
				"}\n" +
				"\n" +
				"if ($opt_a) {\n" +
				"  $opt_cnt++;\n" +
				"  my $i = rand @abc;\n" +
				"  unshift @rand_str, $abc[$i];\n" +
				"\n" +
				"  if ($opt_u) {\n" +
				"    if ($i>=1) {\n" +
				"      $abc[$i-1] = shift @abc;\n" +
				"    } else {\n" +
				"      shift @abc;\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"  unshift (@ch_src, @abc);\n" +
				"}\n" +
				"\n" +
				"if ($opt_A) {\n" +
				"  $opt_cnt++;\n" +
				"  my $i = rand @ABC;\n" +
				"  unshift @rand_str, $ABC[$i];\n" +
				"\n" +
				"  if ($opt_u) {\n" +
				"    if ($i>=1) {\n" +
				"      $ABC[$i-1] = shift @ABC;\n" +
				"    } else {\n" +
				"      shift @ABC;\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"  unshift (@ch_src, @ABC);\n" +
				"}\n" +
				"\n" +
				"if ($opt_n) {\n" +
				"  $opt_cnt++;\n" +
				"  my $i = rand @num;\n" +
				"  unshift @rand_str, $num[$i];\n" +
				"\n" +
				"  if ($opt_u) {\n" +
				"    if ($i>=1) {\n" +
				"      $num[$i-1] = shift @num;\n" +
				"    } else {\n" +
				"      shift @num;\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"  unshift (@ch_src, @num);\n" +
				"}\n" +
				"\n" +
				"if ($opt_s) {\n" +
				"  $opt_cnt++;\n" +
				"  my $i = rand @sym;\n" +
				"  unshift @rand_str, $sym[$i];\n" +
				"\n" +
				"  if ($opt_u) {\n" +
				"    if ($i>=1) {\n" +
				"      $sym[$i-1] = shift @sym;\n" +
				"    } else {\n" +
				"      shift @sym;\n" +
				"    }\n" +
				"  }\n" +
				"\n" +
				"  unshift (@ch_src, @sym);\n" +
				"}\n" +
				"\n" +
				"if ($len < $opt_cnt) {\n" +
				"  print \"The count of characters[$len] should not be smaller \" .\n" +
				"     \"than count of character types[$opt_cnt].\\n\";\n" +
				"  exit -1;\n" +
				"}\n" +
				"\n" +
				"if ($opt_u && $len > (@ch_src + @rand_str)) {\n" +
				"  print \"The total number of characters[\".(@ch_src + @rand_str).\n" +
				"     \"] which could be contained \" .\n" +
				"     \"in password is smaller than the length[$len] of it.\\n\";\n" +
				"  exit -1;\n" +
				"}\n" +
				"\n" +
				"foreach (1..$len-$opt_cnt) {\n" +
				"  my $i = rand @ch_src;\n" +
				"  unshift @rand_str, $ch_src[$i];\n" +
				"\n" +
				"  if ($opt_u) {\n" +
				"    if ($i>=1) {\n" +
				"      $ch_src[$i-1] = shift @ch_src;\n" +
				"    } else {\n" +
				"      shift @ch_src;\n" +
				"    }\n" +
				"  }\n" +
				"}\n" +
				"\n" +
				"foreach (1..$len) {\n" +
				"  my $i = rand @rand_str;\n" +
				"  print $rand_str[$i];\n" +
				"\n" +
				"  if ($i>=1) {\n" +
				"    $rand_str[$i-1] = shift @rand_str;\n" +
				"  } else {\n" +
				"    shift @rand_str;\n" +
				"  }\n" +
				"}\n" +
				"\n" +
				"print \"\\n\";\n" +
				"exit 0;\n";

		List<Map<String, String>> shellTk = ArgumentTokenizer.tokenize(shell2);
		List<Map<String, String>> shellTk2 = ArgumentTokenizer.tokenize(shell2);
		List<Map<String, String>> perlTk = ArgumentTokenizer.tokenize(perl);
//		List<AutoexecScriptLineVo> testVos = markAnnotationLine(getScriptLineList());
		return null;
	}

//	private List<AutoexecScriptLineVo> markAnnotationLine(List<AutoexecScriptLineVo> lines) {
//		for (int i = 0; i < lines.size(); i++) {
//			if (lines.get(i).getContent().startsWith("#")) {
//				lines.get(i).setIsAnnotation(1);
//			} else if (lines.get(i).getContent().startsWith(":<<!")) {
//				while (lines.get(i).getContent().startsWith(":<<!")
//                        || !lines.get(i).getContent().endsWith("!")) {
//					lines.get(i).setIsAnnotation(1);
//					i++;
//					if (lines.get(i).getContent().endsWith("!")) {
//						lines.get(i).setIsAnnotation(1);
//					}
//				}
//			}
//		}
//		return lines;
//	}

	private List<AutoexecScriptLineVo> getScriptLineList() {
		List<AutoexecScriptLineVo> script = new ArrayList<>();
		script.add(new AutoexecScriptLineVo("#path:/sh/", 1));
		script.add(new AutoexecScriptLineVo("#测试网络是否通畅", 2));
		script.add(new AutoexecScriptLineVo("ping -c 1 172.16.13.254 >/dev/null", 3));
		script.add(new AutoexecScriptLineVo("#第一步：关闭selinux和防火墙", 4));
		script.add(new AutoexecScriptLineVo("setenforce 0 >/dev/null", 5));
		script.add(new AutoexecScriptLineVo("systemctl stop firewall >/dev/null", 6));
		script.add(new AutoexecScriptLineVo(":<<!", 7));
		script.add(new AutoexecScriptLineVo("xxxxxx", 8));
		script.add(new AutoexecScriptLineVo("wwwwww", 9));
		script.add(new AutoexecScriptLineVo("qqqqqq", 10));
		script.add(new AutoexecScriptLineVo("eeeeee", 11));
		script.add(new AutoexecScriptLineVo("yyyyyy !", 12));
		script.add(new AutoexecScriptLineVo("#第二步：确认软件是否安装", 13));
		script.add(new AutoexecScriptLineVo("rpm -aq rpcbind >/dev/null", 14));
		script.add(new AutoexecScriptLineVo("if [ $? -eq 0 ];then", 15));
		script.add(new AutoexecScriptLineVo("echo \"rpcbind软件已安装\"", 16));
		script.add(new AutoexecScriptLineVo("else", 17));
		script.add(new AutoexecScriptLineVo("  yum install rpcbind -y >/dev/null && echo \"正在安装软件\"", 18));
		script.add(new AutoexecScriptLineVo("fi", 19));
		script.add(new AutoexecScriptLineVo("echo **********软件已安装**********", 20));
		script.add(new AutoexecScriptLineVo("#第三步:创建和发布共享目录", 21));
		script.add(new AutoexecScriptLineVo(":<<!", 22));
		script.add(new AutoexecScriptLineVo("zzzzzz", 23));
		script.add(new AutoexecScriptLineVo("tttttt", 24));
		script.add(new AutoexecScriptLineVo("uuuuuu", 25));
		script.add(new AutoexecScriptLineVo("!", 26));
		script.add(new AutoexecScriptLineVo(":<<!", 27));
		script.add(new AutoexecScriptLineVo("!", 28));
		return script;
	}


}
