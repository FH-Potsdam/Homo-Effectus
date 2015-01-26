# Homo Effectus

[Video of the installation in action](https://vimeo.com/113909158)

## E

The Goal of the Course titled DIY (Multi) Touch (less) Human Computer Interaction was to develop a Musical Interface which could be controled with gestures like a director of an orchestra directs various instruments and voices. In hopes that we would develop a more expressive interface than regular knobs, sliders and buttons we thought of different gestures and interactions. These ranged from simple right-left-movements to more complex gestures with multiple step movements. After that we tried out different technologies to track body movements. After a few experiments with different Markertechnologies we concluded that within the timelimit the most reliable and simple technology to develop a functional interface would be color detection.
Using simply colored gloves to track movements is of course way more rudimentary and limiting than for example Microsoft's Kinect. It is far more difficult to recognize complex gestures. But we agreed that for our purpose tracking x- and y-position as well as being able to calculate speed and distance between hands would suffice.
On the way from the analog gloves to manipulating digital music, the information has to pass three steps:

1. On the technology side we use a webcam to capture the environment. The image we get from the cam is then being processed by a...well Processing Sketch. The Processing Sketch recognizes different colors as individual Blobs, assigns Ids to every one of them and tracks them to be able to conceive a movement over multiple frames. At the same time the sketch gets the x- and y-position as well as the speed of the individual blobs and sends corresponding OSC signals.

2. The OSC Signals are then received and converted to MIDI Signals by an Application called Osculator.

3. The MIDI signals can then be intercepted and used by any audiosoftware. We used Ableton Live to assign the different MIDI messages to different parameters of different effects. This closes the loop of manipulating audio effects using simple handgestures.

In addition to this we also used the received data from the first Processing sketch to drive a second Processing Sktech which draws patterns which react to the hand movements as well as the music produced by them.

The final product is of course just a prototype. Nevertheless while using it one gets a good impression of the possibilities of gesture based controls.

## D

Unser Ziel des Kurses mit dem Titel DIY (Multi) Touch (less) Human Computer Interaction war es ein Musik Interface zu entwickeln, welches — ähnlich wie ein Dirigent ein Orchester dirigiert — mit Gesten gesteuert werden kann. In der Hoffnung und mit dem Ziel ein expressiveres Interface als Drehknöpfe zu entwickeln haben wir uns verschiedene Gesten und Interaktionsmöglichkeiten überlegt. Diese reichten von simplen rechts-links-Armbewegungen bis zu komplexeren Gesten bei welchen mehrere Bewegungsabläufe involviert waren. Daraufhin haben wir uns mit verschiedenen Technologien zum tracking von Körperteilen auseinandergesetzt. Nach einigen Experimenten mit verschiedenen Markertechnologien wie z.B. Fiduci Marker sind wir zum Schluss gekommen, dass das Tracking von Farben die einfachste und verlässlichste Möglichkeit ist in kurzer Zeit ein funktionales Interface zu entwickeln. Als Eingabemedium haben wir uns darauf geeinigt farbige Handschuhe zu verwenden. Natürlich ist es mit einer derart rudimentären Technik schwierig komplexe Gesten zu erkennen wie dies z.B. mit Microsofts Kinect möglich wäre. Die Parameter x- und y-Koordinate, Geschwindigkeit sowie Distanz der Hände zueinander reichten uns aber für unseren Prototypen aus.
Bis die mit Handschuhen produzierten analogen Daten als Digitale Input Signale in einer Musiksoftware ankommen müssen sie 3 Schritte durchlaufen:

Als Eingabetechnologie verwenden wir eine Webcam, deren Bild von einem Processing Sketch ausgewertet wird. Der Processing Sketch erkennt verschiedene Farben als einzelne Blobs, vergibt diesen eine ID und trackt sie um einen einzelnen Blob über mehrere Frames verfolgen zu können. Gleichzeitig wertet er die x- und y- position sowie die Geschwindigkeit der einzelnen Blobs aus und versendet OSC Signale welche mit den gefundenen Daten angereichert werden.
2. Die OSC Signale werden von einer Applikation namens Osculator in MIDI Signale umgewandelt und weitergeleitet.

3. MIDI Signale werden von vielen Audioprogrammen erkannt und können da weiterverwendet werden. Wir haben für die Weiterverarbeitung Ableton Live verwendet. Damit können wir die empfangenen MIDI Signale beliebigen Effekten zuweisen und so durch einfache Handbewegungen Musik steuern.
Als Zusatz haben wir die empfangenen Daten ebenfalls verwendet um in einem weiteren Processing Sketch eine Animation mit simplen geometrischen Formen zu generieren.
Das Endprodukt ist natürlich nur ein einfacher Prototyp und zeigt nur Ansatzweise auf was möglich ist. Dennoch bekommt man bereits mit dieser simplen Implementierung ein gutes Gefühl für die gestenbasierten Steuerung.


Copyright (c) 2014 Caspar Kirsch, Fabian Dinklage, Flavio Gortana
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

see also http://www.opensource.org/licenses/mit-license.php