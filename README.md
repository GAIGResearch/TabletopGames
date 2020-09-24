# TAG: Tabletop Games Framework

[![license](https://img.shields.io/github/license/GAIGResearch/TabletopGames)](LICENSE)
![top-language](https://img.shields.io/github/languages/top/GAIGResearch/TabletopGames)
![code-size](https://img.shields.io/github/languages/code-size/GAIGResearch/TabletopGames)
[![twitter](https://img.shields.io/twitter/follow/gameai_qmul?style=social)](https://twitter.com/intent/follow?screen_name=gameai_qmul)
[![](https://img.shields.io/github/stars/GAIGResearch/TabletopGames.svg?label=Stars&style=social)](https://github.com/GAIGResearch/TabletopGames)

The Tabletop Games Framework (TAG) is a Java-based benchmark for developing modern board games for AI research.  TAG provides a common skeleton for implementing tabletop games based on a common API for AI agents, a set of components and classes to easily add new games and an import module for defining data in JSON format. At present, this platform includes the implementation of seven different tabletop games that can also be used as an example for further developments. Additionally, TAG also incorporates logging functionality that allows the user to perform a detailed analysis of the game, in terms of action space, branching factor, hidden information, and other measures of interest for Game AI research.
![Pandemic](data/imgs/Pandemic.png)
*Example GUI for Pandemic*

## Games
Currently implemented games:
- [x] Tic-Tac-Toe
- [x] Love Letter (Seiji Kanai 2012)
- [x] Uno (Merle Robbins 1971)
- [x] Virus! (Cabrero and others 2015)
- [x] Exploding Kittens (Inman and others 2015)
- [x] Colt Express (Christophe Raimbault 2014)
- [x] Pandemic (Matt Leacock 2008)

Games in progress:
- [ ] Settlers of Catan (Klaus Teuber 1995)
- [ ] Carcassone (Klaus-Jürgen Wrede 2000)
- [ ] Descent (Jesper Ejsing, John Goodenough, Frank Walls 2005)

## Setting up
The project requires Java with minimum version 8. 

## Getting started

To get started the [wiki](https://github.com/GAIGResearch/TabletopGames/wiki) provides various guides and descriptions of the framework.
Another good resource is our continuously updated paper "DESIGN AND IMPLEMENTATION OF TAG: A TABLETOP GAMES FRAMEWORK".

To cite TAG in your work, cite this paper:
```
[Add citation here]
```

