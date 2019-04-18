package Objects;

import java.awt.Color;
import java.util.*;

import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;

class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int height;
  int width;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  // the time in ticks taken so far playing this game
  int time;
  // the number of moves done in the game
  int moves;
  // the users that can play this game
  ArrayList<User> users;


  // PART 3 : creates a random grid
  LightEmAll(int width, int height) {

    if ((width <= 1 && height <= 1)
        || width < 1 || height < 1) {
      throw new IllegalArgumentException("Board must be more than 1 cell");
    }

    this.height = height;
    this.width = width;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.radius = 0;
    this.time = 0;
    this.moves = 0;

    for (int i = 0; i < width; i++) {
      ArrayList<GamePiece> temp = new ArrayList<GamePiece>();
      for (int x = 0; x < height; x++) {
        GamePiece add = new GamePiece(x, i);
        temp.add(add);
      }
      this.board.add(temp);
    }

    ArrayList<Edge> temp = new ArrayList<Edge>();
    for (int i = 0; i < width - 1; i++) {
      for (int x = 0; x < height; x++) {
        temp.add(new Edge(this.board.get(i).get(x), this.board.get(i + 1).get(x), true));
      }
    }

    for (int i = 0; i < width; i++) {
      for (int x = 0; x < height - 1; x++) {
        temp.add(new Edge(this.board.get(i).get(x), this.board.get(i).get(x + 1), false));
      }
    }

    for (ArrayList<GamePiece> col : this.board) {
      this.nodes.addAll(col);
    }

    this.mst.addAll(this.createMST(this.nodes, temp));

    for (Edge e : this.mst) {
      e.fromNode.addConnections(e.toNode);
      e.toNode.addConnections(e.fromNode);
    }

    int maxDiameter = 0;
    GamePiece source = this.board.get(this.powerCol).get(this.powerRow);

    for (GamePiece target : this.nodes) {
      int diameter = Math.max(maxDiameter,
          this.shortestPath(this.board.get(powerCol).get(powerRow), target));
      if (maxDiameter <= diameter) {
        maxDiameter = diameter;
        source = target;
      }
    }
    
    for (GamePiece target : this.nodes) {
      maxDiameter = Math.max(maxDiameter, this.shortestPath(source, target));
    }
    
    this.radius = (maxDiameter / 2) + 1;
    this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
    
    Random rand = new Random();
    for (GamePiece g : this.nodes) {
      int numRotate = rand.nextInt(4);
      for (int i = 0; i < numRotate; i++) {
        g.rotate();
      }
    }
    this.updateBoard();
  }

  // returns the shortest distance between the given nodes through 
  // in the minimum spanning tree in this using a breadth-first search
  int shortestPath(GamePiece source, GamePiece target) {
    HashMap<GamePiece, Edge> cameFromEdge = new HashMap<GamePiece, Edge>();
    Stack<GamePiece> worklist = new Stack<GamePiece>();
    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();   
    worklist.push(source);
    while (worklist.size() > 0) {
      GamePiece g = worklist.pop();
      if (!visited.contains(g)) {
        visited.add(g);
        if (g.sameGamePiece(target)) {
            return this.reconstruct(cameFromEdge, source, target);
        }
        else {
          for (Edge e : this.mst) {
            if (e.fromNode.sameGamePiece(g) && !visited.contains(e.toNode)) {
              worklist.push(e.toNode);
              cameFromEdge.put(e.toNode, e);
            }
            else if (e.toNode.sameGamePiece(g) && !visited.contains(e.fromNode)) {
              worklist.push(e.fromNode);
              cameFromEdge.put(e.fromNode, new Edge(e.toNode, e.fromNode, false));
            }
          }
        }
      }
    }
    return 0;
  }
  
  // returns the number of steps from the target to the source in the given hashmap
  int reconstruct(HashMap<GamePiece, Edge> cameFromEdge, GamePiece source, GamePiece target) {
    GamePiece node = target;
    int steps = 1;
    while (!node.sameGamePiece(source)) {
      steps++;
      node = cameFromEdge.get(node).fromNode;
    }
    return steps;
  }

  // returns a changed version the given edges to form a
  // minimum spanning tree using Kruskal's algorithm
  ArrayList<Edge> createMST(ArrayList<GamePiece> nodes, ArrayList<Edge> edges) {
    HashMap<GamePiece, GamePiece> representatives = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    this.sortEdges(edges);

    for (GamePiece g : nodes) {
      representatives.put(g, g);
    }

    while (edgesInTree.size() < nodes.size() - 1) {
      if (this.find(edges.get(0).toNode,
          representatives).sameGamePiece(this.find(edges.get(0).fromNode, representatives))) {
        edges.remove(0);
      }
      else {
        edgesInTree.add(edges.get(0));
        representatives.put(
            this.find(edges.get(0).fromNode, representatives),
            this.find(edges.get(0).toNode, representatives));
      }
    }
    return edgesInTree;
  }

  // finds the overall parent of the given node in the given hashmap
  GamePiece find(GamePiece g, HashMap<GamePiece, GamePiece> representatives) {
    if (g.sameGamePiece(representatives.get(g))) {
      return g;
    }
    else {
      return this.find(representatives.get(g), representatives);
    }
  }


  // sorts the given edges in an ascending order of weights
  void sortEdges(ArrayList<Edge> edges) {
    ArrayList<Edge> sorted = new ArrayList<Edge>();
    int size = edges.size();
    while (size != sorted.size()) {
      int minIndex = 0;
      for (int i = 1; i < edges.size(); i++) {
        if (edges.get(minIndex).weight > edges.get(i).weight) {
          minIndex = i;
        }
      }
      sorted.add(edges.get(minIndex));
      edges.remove(minIndex);
    }
    edges.addAll(sorted);
  }

  // creates a fractal-like grid
  LightEmAll(int width, int height, int radius) {

    if ((width <= 1 && height <= 1)
        || width < 1 || height < 1) {
      throw new IllegalArgumentException("Board must be more than 1 cell");
    }

    this.height = height;
    this.width = width;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.powerRow = height / 2;
    this.powerCol = width / 2;
    this.radius = radius;
    this.time = 0;
    this.moves = 0;

    for (int i = 0; i < width; i++) {
      ArrayList<GamePiece> temp = new ArrayList<GamePiece>();
      for (int x = 0; x < height; x++) {
        GamePiece add = new GamePiece(x, i);
        temp.add(add);
      }
      this.board.add(temp);
    }

    this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
    this.generateFBoard(this.board);

    for (ArrayList<GamePiece> col : this.board) {
      this.nodes.addAll(col);
    }

    this.updateBoard();
  }

  // EFFECT: changes the given board to a fractal-like grid
  void generateFBoard(ArrayList<ArrayList<GamePiece>> board) {
    for (int i = 0; i < board.get(0).size() - 1; i++) {
      board.get(0).get(i).bottom = true;
      board.get(board.size() - 1).get(i).bottom = true;
    }
    for (int i = 1; i < board.get(0).size(); i++) {
      board.get(0).get(i).top = true;
      board.get(board.size() - 1).get(i).top = true;
    }
    for (int i = 0; i < board.size() - 1; i++) {
      board.get(i).get(board.get(i).size() - 1).right = true;
    }
    for (int i = 1; i < board.size(); i++) {
      board.get(i).get(board.get(i).size() - 1).left = true;
    }

    if (board.size() > 2 && board.get(0).size() > 2) {
      ArrayList<ArrayList<ArrayList<GamePiece>>> quads = this.generateQuadrants(board);
      for (ArrayList<ArrayList<GamePiece>> quad : quads) {
        this.generateFBoard(quad);
      }
    }
  }

  // creates an arraylist of all the quadrants of the given 2-d array
  <T> ArrayList<ArrayList<ArrayList<T>>> generateQuadrants(ArrayList<ArrayList<T>> grid) {
    ArrayList<ArrayList<ArrayList<T>>> quads = new ArrayList<ArrayList<ArrayList<T>>>();
    ArrayList<ArrayList<T>> quad1 = new ArrayList<ArrayList<T>>();
    ArrayList<ArrayList<T>> quad2 = new ArrayList<ArrayList<T>>();
    ArrayList<ArrayList<T>> quad3 = new ArrayList<ArrayList<T>>();
    ArrayList<ArrayList<T>> quad4 = new ArrayList<ArrayList<T>>();
    int split1 = (grid.size() / 2) + (grid.size() % 2);
    int split2 = (grid.get(0).size() / 2) + (grid.get(0).size() % 2);

    for (int i = 0; i < split1; i++) {
      ArrayList<T> temp = new ArrayList<T>();
      for (int x = 0; x < split2; x++) {
        temp.add(grid.get(i).get(x));
      }
      quad1.add(temp);
    }
    quads.add(quad1);
    for (int i = split1; i < grid.size(); i++) {
      ArrayList<T> temp = new ArrayList<T>();
      for (int x = split2; x < grid.get(0).size(); x++) {
        temp.add(grid.get(i).get(x));
      }
      quad2.add(temp);
    }
    quads.add(quad2);
    for (int i = split1; i < grid.size(); i++) {
      ArrayList<T> temp = new ArrayList<T>();
      for (int x = 0; x < split2; x++) {
        temp.add(grid.get(i).get(x));
      }
      quad3.add(temp);
    }
    quads.add(quad3);
    for (int i = 0; i < split1; i++) {
      ArrayList<T> temp = new ArrayList<T>();
      for (int x = split2; x < grid.get(0).size(); x++) {
        temp.add(grid.get(i).get(x));
      }
      quad4.add(temp);
    }
    quads.add(quad4);

    return quads;
  }

  // creates the scene of this world
  public WorldScene makeScene() {
    WorldImage boardImg = this.draw();
    Double width = boardImg.getWidth();
    Double height = boardImg.getHeight();
    WorldScene bg = new WorldScene(width.intValue(), height.intValue());
    if (this.allConnected()) {
    }
    else {
      bg.placeImageXY(this.draw(), width.intValue() / 2, height.intValue() / 2);
    }
    return bg;
  }

  // draws all the columns and rows of the board in this
  WorldImage draw() {
    WorldImage board = new EmptyImage();
    for (ArrayList<GamePiece> col : this.board) {
      WorldImage colImg = new EmptyImage();
      for (GamePiece g : col) {
        colImg = new AboveImage(colImg, g.draw(this.radius));
      }
      board = new BesideImage(board, colImg);
    }
    Double boardWidth = board.getWidth();
    WorldImage timeCount = new TextImage("Time: " +
        Integer.toString(this.time) + "   ", this.width * 2 + 3, Color.DARK_GRAY);
    WorldImage moveCount = new TextImage("Moves: " +
        Integer.toString(this.moves), this.width * 2 + 3, Color.DARK_GRAY);
    board = new AboveImage(board, new OverlayImage(
        new BesideImage(timeCount, moveCount),
        new RectangleImage(boardWidth.intValue(), 40, OutlineMode.SOLID, Color.gray)));
    return board;
  }

  // rotates the game piece of the board that correlates with the given posn if it exists
  public void onMousePressed(Posn pos, String buttonName) {
    int row = pos.x / 50;
    int col = pos.y / 50;
    if (this.board != null && col < this.board.size() && row < this.board.get(0).size()) {
      if (buttonName.equals("RightButton")) {
        for (int i = 0; i < 3; i++) {
          this.board.get(row).get(col).rotate();
        }
      }
      else {
        this.board.get(row).get(col).rotate();
      }
      ArrayList<GamePiece> temp = new ArrayList<GamePiece>();
      temp.add(this.board.get(row).get(col));
      this.updateGamePiece(row, col, this.radius + 1, temp);
      this.updateBoard();
      this.moves++;
    }
  }

  // EXTRA CREDIT: restarts the game when r is pressed
  // moves the power generator depending on the given key if it is up, down, right or left
  public void onKeyEvent(String key) {
    if (key.equals("up") 
        && this.isValidIndex(this.powerCol, this.powerRow - 1)
        && this.board.get(this.powerCol).get(this.powerRow).top
        && this.board.get(this.powerCol).get(this.powerRow - 1).bottom) {
      this.board.get(this.powerCol).get(this.powerRow).removePowerStation();
      this.powerRow--;
      this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
      this.moves++;
    }
    else if (key.equals("down") 
        && this.isValidIndex(this.powerCol, this.powerRow + 1)
        && this.board.get(this.powerCol).get(this.powerRow).bottom
        && this.board.get(this.powerCol).get(this.powerRow + 1).top) {
      this.board.get(this.powerCol).get(this.powerRow).removePowerStation();
      this.powerRow++;
      this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
      this.moves++;
    }
    else if (key.equals("left") 
        && this.isValidIndex(this.powerCol - 1, this.powerRow)
        && this.board.get(this.powerCol).get(this.powerRow).left
        && this.board.get(this.powerCol - 1).get(this.powerRow).right) {
      this.board.get(this.powerCol).get(this.powerRow).removePowerStation();
      this.powerCol--;
      this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
      this.moves++;
    }
    else if (key.equals("right") 
        && this.isValidIndex(this.powerCol + 1, this.powerRow)
        && this.board.get(this.powerCol).get(this.powerRow).right
        && this.board.get(this.powerCol + 1).get(this.powerRow).left) {
      this.board.get(this.powerCol).get(this.powerRow).removePowerStation();
      this.powerCol++;
      this.board.get(this.powerCol).get(this.powerRow).addPowerStation();
      this.moves++;
    }
    else if (key.equals("r")) {
      LightEmAll temp = new LightEmAll(this.width, this.height);
      this.board = temp.board;
      this.mst = temp.mst;
      this.nodes = temp.nodes;
      this.powerRow = temp.powerRow;
      this.powerCol = temp.powerCol;
    }
    this.updateBoard();
  }

  // are the given numbers valid indexes of the board in this
  boolean isValidIndex(int x, int y) {
    return this.width > x
        && x >= 0
        && this.height > y
        && y >= 0;
  }

  // updates the distance of all the game pieces in the board depending
  // on the distance from the power generator
  void updateBoard() {
    ArrayList<GamePiece> visited = new ArrayList<GamePiece>();
    this.updateGamePiece(this.powerCol, this.powerRow, 0, visited);
    for (ArrayList<GamePiece> col : this.board) {
      for (GamePiece g : col) {
        if (!visited.contains(g)) {
          g.distance = this.radius + 1;
        }
      }
    }
  }

  // updates the distance of the game piece correlating to the given col and row in this board
  // and the game pieces connected to it
  // ACCUMULATOR : accumulates the visited game pieces and the distance to the power generator
  void updateGamePiece(int col, int row, int count, ArrayList<GamePiece> visited) {
    this.board.get(col).get(row).distance = count;
    visited.add(this.board.get(col).get(row));
    if (this.isValidIndex(col - 1, row)
        && this.board.get(col).get(row).left
        && this.board.get(col - 1).get(row).right
        && !visited.contains(this.board.get(col - 1).get(row))) {
      this.updateGamePiece(col - 1, row, count + 1, visited);
    }
    if (this.isValidIndex(col + 1, row)
        && this.board.get(col).get(row).right
        && this.board.get(col + 1).get(row).left
        && !visited.contains(this.board.get(col + 1).get(row))) {
      this.updateGamePiece(col + 1, row, count + 1, visited);
    }
    if (this.isValidIndex(col, row + 1)
        && this.board.get(col).get(row).bottom
        && this.board.get(col).get(row + 1).top
        && !visited.contains(this.board.get(col).get(row + 1))) {
      this.updateGamePiece(col, row + 1, count + 1, visited);
    }
    if (this.isValidIndex(col, row - 1)
        && this.board.get(col).get(row).top
        && this.board.get(col).get(row - 1).bottom
        && !visited.contains(this.board.get(col).get(row - 1))) {
      this.updateGamePiece(col, row - 1, count + 1, visited);
    }
  }

  // adds one to the time in this every tick
  public void onTick() {
    this.time++;
  }
  
  // are all the nodes in this connected
  boolean allConnected() {
    boolean connected = true;
    for (GamePiece g : this.nodes) {
      connected = g.isConnected(this.radius) && connected;
    }
    return connected;
  }
  
}

