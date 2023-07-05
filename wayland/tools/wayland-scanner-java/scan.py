#!/usr/bin/python3

import os
import re
import sys
import textwrap
import xml.etree.ElementTree as ET


class Context:
 pass

class UIntType:
 def __init__(self, enum):
  self.enum = enum

class IntType:
 pass

class FixedType:
 pass

class ArrayType:
 pass

class StrType:
 def __init__(self, n):
  self.nullable = n

class FdType:
 pass

class ObjType:
 def __init__(self, iface, n):
  self.interface = iface
  self.nullable = n

class NewIdType:
 def __init__(self, iface):
  self.interface = iface

def intOrNone(v):
 return None if v is None else int(v)

def getEltText(elt):
 return None if elt is None else elt.text

class Desc:
 @staticmethod
 def parse(ctx, elt):
  t = elt.find('description')
  if t is not None:
   return Desc(t.get('summary'), t.text)
  return Desc(elt.get('summary'), None)

 def __init__(self, s, c):
  self.summary = s
  self.content = c

class Elt:
 def __init__(self, ctx, elt):
  self.name = elt.get('name')
  self.description = Desc.parse(ctx, elt)

class EnumEntry(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.value = elt.get('value')

class Enum(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.entries = [EnumEntry(ctx, v) for v in elt.findall('entry')]

class Arg(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.type = {
   'uint': lambda elt: UIntType(elt.get('enum')),
   'int': lambda elt: IntType(),
   'fixed': lambda elt: FixedType(),
   'array': lambda elt: ArrayType(),
   'string': lambda elt: StrType(elt.get('allow-null') == 'true'),
   'fd': lambda elt: FdType(),
   'object': lambda elt: ObjType(elt.get('interface'), elt.get('allow-null') == 'true'),
   'new_id': lambda elt: NewIdType(elt.get('interface'))
  }[elt.get('type')](elt)

class Method(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.since = intOrNone(elt.get('since'))
  self.dtor = elt.get('type') == 'destructor'
  self.args = [Arg(ctx, v) for v in elt.findall('arg')]

class Interface(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.version = int(elt.get('version'))
  self.requests = [Method(ctx, v) for v in elt.findall('request')]
  self.events = [Method(ctx, v) for v in elt.findall('event')]
  self.enums = [Enum(ctx, v) for v in elt.findall('enum')]
  print(self.name)

class Protocol(Elt):
 def __init__(self, ctx, elt):
  super().__init__(ctx, elt)
  self.copyright = getEltText(elt.find('copyright'))
  self.interfaces = [Interface(ctx, v) for v in elt.findall('interface')]

def asJava(p, pkg):
 protoDirName = p.name.replace('-', '_')

 fixNameRe_ = re.compile('[0-9].*|transient|static|volatile|final|private|protected|public|default')
 fixNameReName = re.compile('interface|class')
 def fixName(v):
  if fixNameReName.match(v):
   return v + 'Name'
  if fixNameRe_.match(v):
   return '_' + v
  return v

 def toComment(v, s=''):
  return '/*' + s + '\n' + textwrap.indent(textwrap.indent(v, ' '), ' *', lambda n: True) + '\n */'

 def formatAsJavadoc(v):
  v = v.strip()
  v = re.sub(r' +', r' ', v)
  v = re.sub(r'^\s*$', r'<p>', v, flags=re.M)
  v = re.sub(r'''(?:(?:[a-z]\w*)?_\w*|[a-z]\w*[0-9]\w*)(?:\.\w+)*|(?<=['"])\w+(?:\.\w+)*(?=['"])''',
             r'{@code \g<0>}', v)
  return v

 def toJavadoc(v, s=''):
  return toComment(v, '*')

 def descToStr(v):
  if v is None:
   return ''
  return formatAsJavadoc('\n\n'.join(filter(lambda i: len(i) > 0,
                                            map(lambda i: textwrap.dedent(i).strip(),
                                                filter(lambda i: isinstance(i, str),
                                                       (v.summary, v.content))))))

 def argDescToStr(v):
  if v is None:
   return '...'
  return '...' if v.summary is None else formatAsJavadoc(v.summary)

 imports = '''package {pkg}.protocol.{proto};

{c}

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

import {pkg}.protocol_core.WlInterface;
import {pkg}.protocol.wayland.*;

'''.format(pkg=pkg, proto=protoDirName, c=toComment(textwrap.dedent(p.copyright).strip()))
 types = {
  UIntType: lambda arg: 'long',
  IntType: lambda arg: 'int',
  FixedType: lambda arg: 'float',
  ArrayType: lambda arg: '@NonNull int[]',
  StrType: lambda arg: ('@INullable @Nullable' if arg.nullable else '@NonNull') + ' String',
  FdType: lambda arg: '@NonNull FileDescriptor',
  ObjType: lambda arg: ('@INullable @Nullable' if arg.nullable else '@NonNull') + ' ' + ('WlInterface' if arg.interface is None else arg.interface),
  NewIdType: lambda arg: ('' if arg.interface is None else ('@Iface(%s.class) ' % arg.interface)) + '@NonNull NewId',
 }

 def writeMethods(f, mm):
  i = 0
  for m in mm:
   f.write('\n' + textwrap.indent(toJavadoc(
    descToStr(m.description) + ('\n\n' if len(m.args) > 0 else '') + '\n'.join(
     '@param ' + fixName(arg.name) + ' ' + argDescToStr(arg.description) for arg in m.args)),
                                  ' ' * 8) + '\n')
   f.write('        @IMethod(%u)\n' % i)
   if m.since is not None:
    f.write('        @ISince(%u)\n' % m.since)
   if m.dtor:
    f.write('        @IDtor\n')
   f.write('        void %s(' % fixName(m.name))
   f.write(', '.join(types[type(arg.type)](arg.type) + ' ' + fixName(arg.name) for arg in m.args))
   f.write(');\n')
   i += 1

 try:
  os.mkdir(protoDirName)
 except FileExistsError:
  pass
 for iface in p.interfaces:
  with open('%s/%s.java' % (protoDirName, iface.name), 'w') as f:
   f.write(imports)
   f.write(toJavadoc(descToStr(iface.description)) + '\n')
   f.write('public class %s extends WlInterface' % iface.name)
   f.write(f'<{iface.name}.Requests, {iface.name}.Events>')
   f.write(' {\n')
   f.write('    public static final int version = %u;\n' % iface.version)
   f.write('\n    public interface Requests extends WlInterface.Requests {\n')
   writeMethods(f, iface.requests)
   f.write('    }\n')
   f.write('\n    public interface Events extends WlInterface.Events {\n')
   writeMethods(f, iface.events)
   f.write('    }\n')
   f.write('\n    public static final class Enums {\n')
   f.write('        private Enums() {\n        }\n')
   for enum in iface.enums:
    f.write('\n')
    if enum.description.summary is not None:
     f.write(textwrap.indent(toJavadoc(argDescToStr(enum.description)), ' ' * 8) + '\n')
    f.write('        public static final class %s {\n' % fixName(enum.name.capitalize()))
    f.write('            private %s() {\n            }\n' % fixName(enum.name.capitalize()))
    for ee in enum.entries:
     f.write('\n')
     if ee.description.summary is not None:
      f.write(textwrap.indent(toJavadoc(ee.description.summary), ' ' * 12) + '\n')
     f.write('            public static final int %s = %s;\n' % (fixName(ee.name), ee.value))
    f.write('        }\n')
   f.write('    }\n')
   f.write('}\n')

def main():
 inputFile = sys.argv[1]
 tree = ET.parse(inputFile)
 root = tree.getroot()
 if root.tag != 'protocol':
  raise ValueError('Not a protocol file')
 ctx = Context()
 p = Protocol(ctx, root)
 asJava(p, 'green_green_avk.wayland')

if __name__ == "__main__":
 main()
